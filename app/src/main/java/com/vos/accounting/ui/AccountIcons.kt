package com.vos.accounting.ui

import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.vos.accounting.data.ACCOUNT_ICON_CUSTOM_PREFIX
import com.vos.accounting.R
import com.vos.accounting.model.AccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val ACCOUNT_ICON_SPRITE_CELL = 96

/**
 * 表示账户图标选择页中的一个本地图标。
 */
data class AccountIconItem(
    val key: String,
    val title: String,
    val row: Int,
    val column: Int,
)

/**
 * 表示账户图标选择页中的一个语义分组。
 */
data class AccountIconSection(
    val title: String,
    val icons: List<AccountIconItem>,
)

private val generalAccountIcons = listOf(
    "cash" to "现金", "debit_card" to "储蓄卡", "credit_card" to "信用卡",
    "stored_value_card" to "储值卡", "online_account" to "网络账户",
    "investment_account" to "投资账户", "virtual_account" to "虚拟账户",
    "debt" to "债务", "housing_fund" to "公积金", "medical_insurance" to "医保",
    "fuel" to "加油", "charging" to "充电", "nfc" to "NFC",
)

private val internetAccountIcons = listOf(
    "alipay" to "支付宝", "yu_e_bao" to "余额宝", "huabei" to "花呗", "jiebei" to "借呗", "xiao_he_bao" to "小荷包",
    "yu_li_bao" to "余利宝", "ant_fortune" to "蚂蚁财富", "mybank_loan" to "网商贷", "wechat_wallet" to "微信钱包", "ling_qian_tong" to "零钱通",
    "li_cai_tong" to "理财通", "weili_dai" to "微粒贷", "fen_fu" to "分付", "xiao_e_hua_qian" to "小鹅花钱", "union_pay" to "云闪付",
    "jd_finance" to "京东金融", "jd_vault" to "京东小金库", "e_cny" to "数字人民币", "bestpay" to "翼支付", "pinduoduo" to "拼多多",
    "qq_wallet" to "QQ钱包", "meituan_monthly" to "美团月付", "jd_baitiao" to "京东白条", "eleme" to "饿了么", "du_xiaoman" to "度小满",
    "baidu_credit" to "百度有钱花", "dou_installment" to "Dou分期", "qihu_loan" to "360借条", "wacai" to "挖财", "eastmoney" to "东方财富",
    "douyin" to "抖音", "kuaishou" to "快手", "bilibili" to "哔哩哔哩", "weibo" to "微博", "vipshop" to "唯品会",
    "yonghui" to "永辉", "suning" to "苏宁", "xiaomi" to "小米", "huawei" to "华为", "china_mobile" to "中国移动",
    "china_unicom" to "中国联通", "china_telecom" to "中国电信",
)

private val bankAccountIcons = listOf(
    "baoshang_bank" to "包商银行", "bank_of_beijing" to "北京银行", "aibank" to "百信银行", "bank_of_chongqing" to "重庆银行", "bank_of_changsha" to "长沙银行",
    "bank_of_chengdu" to "成都银行", "dazhou_bank" to "达州银行", "icbc" to "工商银行", "bank_of_gansu" to "甘肃银行", "everbright_bank" to "光大银行",
    "cgb" to "广发银行", "bank_of_guangzhou" to "广州银行", "bank_of_guizhou" to "贵州银行", "bank_of_ganzhou" to "赣州银行", "bank_of_hebei" to "河北银行",
    "bank_of_hubei" to "湖北银行", "harbin_bank" to "哈尔滨银行", "hsbc" to "汇丰银行", "huarong_xiangjiang_bank" to "华融湘江银行", "bank_of_hangzhou" to "杭州银行",
    "eg_bank" to "恒丰银行", "citibank" to "花旗银行", "huishang_bank" to "徽商银行", "huaxia_bank" to "华夏银行", "ccb" to "建设银行",
    "bank_of_jiangsu" to "江苏银行", "jiujiang_bank" to "九江银行", "bank_of_communications" to "交通银行", "bank_of_jiangxi" to "江西银行", "bank_of_kunlun" to "昆仑银行",
    "bank_of_lanzhou" to "兰州银行", "bank_of_liuzhou" to "柳州银行", "cmbc" to "民生银行", "rural_credit" to "农村信用社", "bank_of_nanjing" to "南京银行",
    "bank_of_ningbo" to "宁波银行", "abc" to "农业银行", "ping_an_bank" to "平安银行", "spdb" to "浦发银行", "bank_of_qingdao" to "青岛银行",
    "bank_of_qinghai" to "青海银行", "qilu_bank" to "齐鲁银行", "bank_of_shanghai" to "上海银行", "shengjing_bank" to "盛京银行", "bank_of_shanxi" to "山西银行",
    "bank_of_suzhou" to "苏州银行", "bank_of_taizhou" to "台州银行", "urumqi_bank" to "乌鲁木齐银行", "mybank" to "网商银行", "webank" to "微众银行",
    "wenzhou_bank" to "温州银行", "xian_bank" to "西安银行", "xiamen_bank" to "厦门银行", "cib" to "兴业银行", "postal_savings_bank" to "邮政储蓄",
    "zhongbang_bank" to "众邦银行", "huarun_bank" to "珠海华润银行", "cmb" to "招商银行", "czbank" to "浙商银行", "boc" to "中国银行",
    "citic_bank" to "中信银行", "bank_of_zhongyuan" to "中原银行", "bank_of_zhengzhou" to "郑州银行", "mastercard" to "万事达卡", "american_express" to "美国运通",
    "visa" to "VISA", "jcb" to "JCB",
)

/**
 * 将按截图顺序排列的名称映射到精灵图坐标。
 */
private fun buildAccountIcons(
    values: List<Pair<String, String>>,
    firstRow: Int,
): List<AccountIconItem> = values.mapIndexed { index, value ->
    AccountIconItem(
        key = value.first,
        title = value.second,
        row = firstRow + index / 5,
        column = index % 5,
    )
}

val accountIconSections = listOf(
    AccountIconSection("通用", buildAccountIcons(generalAccountIcons, 0)),
    AccountIconSection("互联网", buildAccountIcons(internetAccountIcons, 3)),
    AccountIconSection("银行", buildAccountIcons(bankAccountIcons, 12)),
)

private val accountIconsByKey = accountIconSections
    .flatMap(AccountIconSection::icons)
    .associateBy(AccountIconItem::key)

/**
 * 返回账户类型首次建立账户时使用的默认图标标识。
 */
fun defaultAccountIconKey(type: AccountType): String = when (type) {
    AccountType.CASH -> "cash"
    AccountType.BANK_CARD -> "debit_card"
    AccountType.CREDIT -> "credit_card"
    AccountType.ONLINE -> "online_account"
    AccountType.INVESTMENT -> "investment_account"
    AccountType.STORED_VALUE -> "stored_value_card"
    AccountType.VIRTUAL -> "virtual_account"
}

/**
 * 绘制本地图标或用户选择的自定义账户图标。
 */
@Composable
fun AccountIcon(
    iconKey: String,
    modifier: Modifier = Modifier,
) {
    if (iconKey.startsWith(ACCOUNT_ICON_CUSTOM_PREFIX)) {
        CustomAccountIcon(iconKey.removePrefix(ACCOUNT_ICON_CUSTOM_PREFIX), modifier)
    } else {
        BundledAccountIcon(accountIconsByKey[iconKey] ?: accountIconsByKey.getValue("cash"), modifier)
    }
}

/**
 * 从本地精灵图绘制一个内置账户图标。
 */
@Composable
private fun BundledAccountIcon(
    item: AccountIconItem,
    modifier: Modifier,
) {
    val sprite = ImageBitmap.imageResource(R.drawable.account_icons)
    Canvas(modifier = modifier) {
        drawImage(
            image = sprite,
            srcOffset = IntOffset(
                item.column * ACCOUNT_ICON_SPRITE_CELL,
                item.row * ACCOUNT_ICON_SPRITE_CELL,
            ),
            srcSize = IntSize(ACCOUNT_ICON_SPRITE_CELL, ACCOUNT_ICON_SPRITE_CELL),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        )
    }
}

/**
 * 解码并裁剪用户通过系统选择器授权的自定义账户图片。
 */
@Composable
private fun CustomAccountIcon(
    uriText: String,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uriText) {
        value = withContext(Dispatchers.IO) {
            val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uriText))
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = 256f / maxOf(info.size.width, info.size.height)
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }.asImageBitmap()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier.squircleClip(10.dp),
            contentScale = ContentScale.Crop,
        )
    }
}

/**
 * 展示包含全部内置图标及右上角自定义入口的账户图标选择页。
 */
@Composable
fun AccountIconScreen(
    selectedKey: String,
    backdrop: LayerBackdrop,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val customIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            onSelect("$ACCOUNT_ICON_CUSTOM_PREFIX$it")
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AccountingBlurTopBar(backdrop = backdrop) {
                TopAppBar(
                    title = "账户图标",
                    color = Color.Transparent,
                    navigationIcon = {
                        IconButton(onClick = onBack, minWidth = 35.dp, minHeight = 35.dp) {
                            Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                        }
                    },
                    actions = {
                        TopBarIconAction(MiuixIcons.Photos, "自定义账户图标") {
    customIconLauncher.launch(arrayOf("image/*"))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    actionIconPadding = TOP_BAR_ACTION_END_PADDING,
                )
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                ),
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 800.dp)
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
            ) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                accountIconSections.forEach { section ->
                    item(key = section.title) {
                        AccountIconSectionCard(
                            section = section,
                            selectedKey = selectedKey,
                            onSelect = onSelect,
                        )
                    }
                }
                item {
                    Spacer(
                        modifier = Modifier
                            .height(24.dp)
                            .navigationBarsPadding(),
                    )
                }
            }
        }
    }
}

/**
 * 展示一个连续 Card 内的五列账户图标网格。
 */
@Composable
private fun AccountIconSectionCard(
    section: AccountIconSection,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Text(
            text = section.title,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MiuixTheme.textStyles.headline2,
        )
        section.icons.chunked(5).forEach { rowIcons ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowIcons.forEach { item ->
                    AccountIconCell(
                        item = item,
                        selected = selectedKey == item.key,
                        onClick = { onSelect(item.key) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(5 - rowIcons.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 展示一个可选择的账户图标与名称。
 */
@Composable
private fun AccountIconCell(
    item: AccountIconItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .squircleBackground(
                color = if (selected) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
                cornerRadius = 14.dp,
            )
            .squircleClip(14.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AccountIcon(iconKey = item.key, modifier = Modifier.size(34.dp))
        Text(
            text = item.title,
            maxLines = 1,
            style = MiuixTheme.textStyles.body2,
        )
    }
}
