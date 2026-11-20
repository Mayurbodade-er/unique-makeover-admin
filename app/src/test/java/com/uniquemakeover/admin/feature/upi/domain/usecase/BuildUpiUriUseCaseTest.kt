package com.uniquemakeover.admin.feature.upi.domain.usecase

import com.uniquemakeover.admin.feature.upi.domain.model.UpiRequest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BuildUpiUriUseCaseTest {

    private lateinit var useCase: BuildUpiUriUseCase

    @Before
    fun setUp() {
        useCase = BuildUpiUriUseCase()
    }

    @Test
    fun standardRequest_formatsNpciUriCorrectly() {
        val request = UpiRequest(
            upiId = "uniquemakeover@upi",
            payeeName = "Unique Makeover",
            amount = 850,
            transactionNote = "Pooja - Balance Payment"
        )

        val uri = useCase(request)

        val expected = "upi://pay?pa=uniquemakeover@upi&pn=Unique%20Makeover" +
            "&am=850.00&cu=INR&tn=Pooja%20-%20Balance%20Payment"
        assertEquals(expected, uri)
    }

    @Test
    fun zeroAmount_omitsAmountParam() {
        val request = UpiRequest(
            upiId = "uniquemakeover@upi",
            payeeName = "Unique Makeover",
            amount = 0,
            transactionNote = "General Payment"
        )

        val uri = useCase(request)

        val expected = "upi://pay?pa=uniquemakeover@upi&pn=Unique%20Makeover&cu=INR&tn=General%20Payment"
        assertEquals(expected, uri)
    }

    @Test
    fun specialCharactersInNameAndNote_percentEncodedProperly() {
        val request = UpiRequest(
            upiId = "unique@sbi",
            payeeName = "Unique & Chic 'Salon'",
            amount = 1200,
            transactionNote = "Facial + Haircut & Spa"
        )

        val uri = useCase(request)

        // & must be %26, spaces must be %20, + must be %2B
        val expected = "upi://pay?pa=unique@sbi&pn=Unique%20%26%20Chic%20%27Salon%27" +
            "&am=1200.00&cu=INR&tn=Facial%20%2B%20Haircut%20%26%20Spa"
        assertEquals(expected, uri)
    }
}
