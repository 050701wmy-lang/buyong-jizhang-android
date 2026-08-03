package com.vos.accounting.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** 验证币种汇率定点换算不会引入浮点金额误差。 */
class CurrencyRatesTest {
    /** 验证一百美元按给定汇率折算并四舍五入到人民币分。 */
    @Test
    fun convertUsdMinorToCnyMinor() {
        assertEquals(67_598L, convertToCnyMinor(10_000L, 675_977_000L))
    }

    /** 验证人民币金额在本位币汇率下保持不变。 */
    @Test
    fun keepCnyMinorUnchanged() {
        assertEquals(12_345L, convertToCnyMinor(12_345L, CURRENCY_RATE_SCALE))
    }
}
