package com.vos.accounting.auto

import android.content.Context
import android.content.pm.PackageManager
import com.google.re2j.Pattern
import com.vos.accounting.data.AccountingRepository
import com.vos.accounting.data.AutoRulePackEntity
import com.vos.accounting.model.AutoBookkeepingCapture
import com.vos.accounting.model.AutoCaptureSource
import com.vos.accounting.model.MAX_AMOUNT_MINOR
import com.vos.accounting.model.PaymentProvider
import com.vos.accounting.model.TransactionType
import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val BUILTIN_RULE_ASSET = "auto_bookkeeping/rule_pack_v1.json"
const val MAX_RULE_PACK_BYTES = 1024 * 1024

/** 声明规则字段支持的有限提取方式。 */
@Serializable
enum class RuleExtractorKind {
    LABEL,
    REGEX,
    JSON_PATH,
    CONSTANT,
}

/** 声明字段提取后的可组合规范化操作。 */
@Serializable
enum class RuleNormalization {
    TRIM,
    NORMALIZE_SPACE,
    REMOVE_COMMAS,
    LOWERCASE,
    UPPERCASE,
}

/** 描述一条字段提取器，不允许执行脚本或任意代码。 */
@Serializable
data class RuleFieldExtractorV1(
    val kind: RuleExtractorKind,
    val labels: List<String> = emptyList(),
    val pattern: String? = null,
    val group: Int = 1,
    @SerialName("json_path")
    val jsonPath: String? = null,
    val value: String? = null,
    val normalizations: List<RuleNormalization> = listOf(RuleNormalization.TRIM),
)

/** 描述一个支付场景的匹配条件、方向与字段提取方式。 */
@Serializable
data class AutoBookkeepingRuleV1(
    val id: String,
    val provider: PaymentProvider,
    val sources: Set<AutoCaptureSource>,
    val priority: Int = 0,
    @SerialName("package_names")
    val packageNames: Set<String>,
    @SerialName("min_version_code")
    val minVersionCode: Long? = null,
    @SerialName("max_version_code")
    val maxVersionCode: Long? = null,
    @SerialName("activity_patterns")
    val activityPatterns: List<String> = emptyList(),
    @SerialName("required_keywords")
    val requiredKeywords: List<String> = emptyList(),
    @SerialName("required_any_keywords")
    val requiredAnyKeywords: List<String> = emptyList(),
    @SerialName("excluded_keywords")
    val excludedKeywords: List<String> = emptyList(),
    val type: TransactionType,
    val fields: Map<String, List<RuleFieldExtractorV1>>,
)

/** 表示可以从 APK 或用户导入内容加载的第一版规则包。 */
@Serializable
data class RulePackV1(
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("pack_id")
    val packId: String,
    @SerialName("pack_version")
    val packVersion: Int,
    val rules: List<AutoBookkeepingRuleV1>,
)

/** 承载一次规则匹配所需、且不会被持久化的最小输入。 */
data class RuleInput(
    val packageName: String,
    val source: AutoCaptureSource,
    val occurredAt: Long,
    val textParts: List<String>,
    val activityName: String? = null,
    val jsonPayload: String? = null,
)

/** 校验、加载并执行内置规则与用户导入规则。 */
class AutoBookkeepingRuleEngine(
    private val context: Context,
    private val repository: AccountingRepository,
) {
    private val json = Json { ignoreUnknownKeys = false; explicitNulls = false }

    /** 依次执行用户规则和内置规则，返回第一条完整匹配结果。 */
    suspend fun parse(input: RuleInput): AutoBookkeepingCapture? {
        val versionCode = packageVersionCode(input.packageName)
        loadRulePacks().forEach { pack ->
            pack.rules.sortedByDescending(AutoBookkeepingRuleV1::priority).forEach { rule ->
                if (ruleApplies(rule, input, versionCode)) {
                    extractCapture(pack, rule, input)?.let { return it }
                }
            }
        }
        return null
    }

    /** 解析并完整校验用户规则，校验完成前不修改数据库。 */
    fun validateImportedRulePack(bytes: ByteArray): RulePackV1 {
        require(bytes.isNotEmpty()) { "规则文件为空" }
        require(bytes.size <= MAX_RULE_PACK_BYTES) { "规则文件不能超过 1MiB" }
        val text = bytes.toString(Charsets.UTF_8)
        val pack = json.decodeFromString(RulePackV1.serializer(), text)
        validateRulePack(pack)
        return pack
    }

    /** 校验完成后原子激活用户导入规则包。 */
    suspend fun importRulePack(bytes: ByteArray): AutoRulePackEntity {
        val pack = validateImportedRulePack(bytes)
        val activeRuleIds = repository.getActiveAutoRulePacks()
            .filter { it.packId != pack.packId }
            .flatMap { entity ->
                runCatching {
                    json.decodeFromString(RulePackV1.serializer(), entity.jsonContent).rules.map(AutoBookkeepingRuleV1::id)
                }.getOrDefault(emptyList())
            }
            .toSet()
        require(pack.rules.none { it.id in activeRuleIds }) { "规则标识与已激活规则重复" }
        val entity = AutoRulePackEntity(
            packId = pack.packId,
            packVersion = pack.packVersion,
            jsonContent = bytes.toString(Charsets.UTF_8),
            importedAt = System.currentTimeMillis(),
        )
        repository.activateAutoRulePack(entity)
        return entity
    }

    /** 校验规则包结构、重复标识、字段和所有线性正则。 */
    fun validateRulePack(pack: RulePackV1) {
        require(pack.schemaVersion == 1) { "仅支持 RulePackV1" }
        require(pack.packId.matches(Regex("[a-z0-9_.-]{1,80}"))) { "规则包标识不合法" }
        require(pack.packVersion > 0) { "规则包版本必须大于零" }
        require(pack.rules.isNotEmpty()) { "规则包没有规则" }
        require(pack.rules.map(AutoBookkeepingRuleV1::id).distinct().size == pack.rules.size) {
            "规则标识重复"
        }
        pack.rules.forEach { rule ->
            require(rule.id.matches(Regex("[a-z0-9_.-]{1,100}"))) { "规则标识不合法" }
            require(rule.packageNames.isNotEmpty()) { "规则缺少包名" }
            require(rule.sources.isNotEmpty()) { "规则缺少来源" }
            require(rule.fields[FIELD_AMOUNT].orEmpty().isNotEmpty()) { "规则缺少金额提取器" }
            rule.activityPatterns.forEach(::compileLinearPattern)
            rule.fields.values.flatten().forEach { extractor ->
                when (extractor.kind) {
                    RuleExtractorKind.LABEL -> require(extractor.labels.isNotEmpty()) { "标签提取器缺少标签" }
                    RuleExtractorKind.REGEX -> compileLinearPattern(requireNotNull(extractor.pattern))
                    RuleExtractorKind.JSON_PATH -> validateJsonPath(requireNotNull(extractor.jsonPath))
                    RuleExtractorKind.CONSTANT -> requireNotNull(extractor.value)
                }
                require(extractor.group in 0..16) { "正则分组越界" }
            }
        }
    }

    /** 读取用户激活规则和 APK 内置规则，用户规则始终优先。 */
    private suspend fun loadRulePacks(): List<RulePackV1> {
        val imported = repository.getActiveAutoRulePacks().mapNotNull { entity ->
            runCatching {
                json.decodeFromString(RulePackV1.serializer(), entity.jsonContent).also(::validateRulePack)
            }.getOrNull()
        }
        val builtinText = context.assets.open(BUILTIN_RULE_ASSET).bufferedReader().use { it.readText() }
        val builtin = json.decodeFromString(RulePackV1.serializer(), builtinText).also(::validateRulePack)
        return imported + builtin
    }

    /** 判断规则的平台、来源、版本、页面和关键词约束是否全部成立。 */
    private fun ruleApplies(
        rule: AutoBookkeepingRuleV1,
        input: RuleInput,
        versionCode: Long?,
    ): Boolean {
        if (input.packageName !in rule.packageNames || input.source !in rule.sources) return false
        if (rule.minVersionCode != null && (versionCode == null || versionCode < rule.minVersionCode)) return false
        if (rule.maxVersionCode != null && (versionCode == null || versionCode > rule.maxVersionCode)) return false
        if (rule.activityPatterns.isNotEmpty()) {
            val activity = input.activityName ?: return false
            if (rule.activityPatterns.none { compileLinearPattern(it).matcher(activity).find() }) return false
        }
        val text = input.textParts.joinToString(" ")
        if (rule.requiredKeywords.any { it !in text }) return false
        if (rule.requiredAnyKeywords.isNotEmpty() && rule.requiredAnyKeywords.none(text::contains)) return false
        if (rule.excludedKeywords.any(text::contains)) return false
        return true
    }

    /** 从匹配规则提取标准化账单字段并标注字段来源。 */
    private fun extractCapture(
        pack: RulePackV1,
        rule: AutoBookkeepingRuleV1,
        input: RuleInput,
    ): AutoBookkeepingCapture? {
        val lines = input.textParts.map(String::trim).filter(String::isNotEmpty).distinct()
        val jsonRoot = input.jsonPayload?.let { payload -> runCatching { json.parseToJsonElement(payload) }.getOrNull() }
        val amountText = extractFirst(rule.fields[FIELD_AMOUNT], lines, jsonRoot) ?: return null
        val amountMinor = parseAmountMinor(amountText) ?: return null
        val merchant = extractFirst(rule.fields[FIELD_MERCHANT], lines, jsonRoot).orEmpty()
        val paymentMethod = extractFirst(rule.fields[FIELD_PAYMENT_METHOD], lines, jsonRoot).orEmpty()
        val externalKey = extractFirst(rule.fields[FIELD_EXTERNAL_KEY], lines, jsonRoot)
        val currency = extractFirst(rule.fields[FIELD_CURRENCY], lines, jsonRoot)
            ?.lowercase()
            ?.takeIf { it.matches(Regex("[a-z]{3}")) }
            ?: "cny"
        val note = extractFirst(rule.fields[FIELD_NOTE], lines, jsonRoot).orEmpty()
        val provenance = buildMap {
            put(FIELD_TYPE, input.source)
            put(FIELD_AMOUNT, input.source)
            if (merchant.isNotEmpty()) put(FIELD_MERCHANT, input.source)
            if (paymentMethod.isNotEmpty()) put(FIELD_PAYMENT_METHOD, input.source)
            if (externalKey != null) put(FIELD_EXTERNAL_KEY, input.source)
        }
        return AutoBookkeepingCapture(
            provider = rule.provider,
            source = input.source,
            type = rule.type,
            amountMinor = amountMinor,
            currencyKey = currency,
            merchant = merchant,
            note = note,
            occurredAt = input.occurredAt,
            paymentMethodKey = paymentMethod,
            externalKeyHash = externalKey?.let { hashExternalKey(rule.provider, it) },
            ruleId = rule.id,
            rulePackVersion = pack.packVersion,
            fieldProvenance = provenance,
            aiAssisted = input.source == AutoCaptureSource.CLOUD_AI,
        )
    }

    /** 按声明顺序返回首个非空字段值。 */
    private fun extractFirst(
        extractors: List<RuleFieldExtractorV1>?,
        lines: List<String>,
        jsonRoot: JsonElement?,
    ): String? = extractors.orEmpty().firstNotNullOfOrNull { extractor ->
        extract(extractor, lines, jsonRoot)?.let { normalize(it, extractor.normalizations) }?.takeIf(String::isNotEmpty)
    }

    /** 执行一条受限字段提取器。 */
    private fun extract(
        extractor: RuleFieldExtractorV1,
        lines: List<String>,
        jsonRoot: JsonElement?,
    ): String? = when (extractor.kind) {
        RuleExtractorKind.LABEL -> findLabeledValue(lines, extractor.labels)
        RuleExtractorKind.REGEX -> {
            val matcher = compileLinearPattern(requireNotNull(extractor.pattern)).matcher(lines.joinToString("\n"))
            if (matcher.find() && extractor.group <= matcher.groupCount()) matcher.group(extractor.group) else null
        }
        RuleExtractorKind.JSON_PATH -> jsonRoot?.let { root -> safeJsonPath(root, requireNotNull(extractor.jsonPath)) }
        RuleExtractorKind.CONSTANT -> extractor.value
    }

    /** 返回安装包版本号，查询失败时让带版本约束的规则安全关闭。 */
    private fun packageVersionCode(packageName: String): Long? = runCatching {
        context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode
    }.getOrNull()
}

/** 对一条值执行规则中声明的有限规范化操作。 */
private fun normalize(value: String, operations: List<RuleNormalization>): String = operations.fold(value) { text, operation ->
    when (operation) {
        RuleNormalization.TRIM -> text.trim()
        RuleNormalization.NORMALIZE_SPACE -> text.trim().replace(Regex("\\s+"), " ")
        RuleNormalization.REMOVE_COMMAS -> text.replace(",", "")
        RuleNormalization.LOWERCASE -> text.lowercase()
        RuleNormalization.UPPERCASE -> text.uppercase()
    }
}

/** 读取同节点、冒号拼接或相邻节点形式的标签值。 */
private fun findLabeledValue(lines: List<String>, labels: List<String>): String? {
    lines.forEachIndexed { index, line ->
        labels.forEach { label ->
            if (line == label) return lines.getOrNull(index + 1)?.takeIf(String::isNotBlank)
            if (line.startsWith(label)) {
                val value = line.removePrefix(label).trimStart('：', ':', ' ', '\t')
                if (value.isNotEmpty()) return value
            }
        }
    }
    return null
}

/** 只允许点分对象键和数字数组下标的安全 JSON 路径。 */
private fun safeJsonPath(root: JsonElement, path: String): String? {
    validateJsonPath(path)
    val value = path.split('.').fold(root as JsonElement?) { current, segment ->
        when (current) {
            is JsonObject -> current[segment]
            is JsonArray -> segment.toIntOrNull()?.let(current::getOrNull)
            else -> null
        }
    }
    return (value as? JsonPrimitive)?.contentOrNull
}

/** 拒绝空路径、特殊表达式和过深 JSON 访问。 */
private fun validateJsonPath(path: String) {
    val segments = path.split('.')
    require(segments.size in 1..12 && segments.all { it.matches(Regex("[A-Za-z0-9_-]{1,80}")) }) {
        "JSON 路径不合法"
    }
}

/** 编译线性时间正则并把语法错误转成导入失败。 */
private fun compileLinearPattern(pattern: String): Pattern {
    require(pattern.length <= 1024) { "正则表达式过长" }
    return Pattern.compile(pattern)
}

/** 把十进制元金额安全转换为最小货币单位。 */
private fun parseAmountMinor(value: String): Long? = runCatching {
    BigDecimal(value.replace(",", "")).movePointRight(2).longValueExact()
}.getOrNull()?.takeIf { it in 1..MAX_AMOUNT_MINOR }

private const val FIELD_TYPE = "type"
private const val FIELD_AMOUNT = "amount"
private const val FIELD_MERCHANT = "merchant"
private const val FIELD_PAYMENT_METHOD = "payment_method"
private const val FIELD_EXTERNAL_KEY = "external_key"
private const val FIELD_CURRENCY = "currency"
private const val FIELD_NOTE = "note"
