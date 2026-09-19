package com.example.data.model

data class AdvisoryFirm(
    val id: String,
    val name: String,
    val tag: String,
    val description: String,
    val searchQuery: String
) {
    companion object {
        val ALL_FIRMS = AdvisoryFirm(
            id = "all_advisory",
            name = "All Advisory Firms",
            tag = "All",
            description = "Reports across Forrester, Everest, Gartner, McKinsey, BCG & IDC",
            searchQuery = "Forrester OR Everest Group OR Gartner OR McKinsey OR BCG research report"
        )

        val DEFAULT_FIRMS = listOf(
            AdvisoryFirm(
                id = "forrester",
                name = "Forrester Research",
                tag = "Forrester Wave",
                description = "Forrester Waves, tech tide, CIO priorities & vendor assessments",
                searchQuery = "Forrester Research OR Forrester Wave"
            ),
            AdvisoryFirm(
                id = "everest",
                name = "Everest Group",
                tag = "PEAK Matrix",
                description = "Everest Group PEAK Matrix, IT services & business process market research",
                searchQuery = "Everest Group PEAK Matrix OR Everest Group report"
            ),
            AdvisoryFirm(
                id = "gartner",
                name = "Gartner",
                tag = "Magic Quadrant",
                description = "Magic Quadrants, Hype Cycles, IT spending forecasts & market guides",
                searchQuery = "Gartner Magic Quadrant OR Gartner research"
            ),
            AdvisoryFirm(
                id = "mckinsey",
                name = "McKinsey & Company",
                tag = "MGI Insights",
                description = "McKinsey Global Institute (MGI), state of AI & executive strategy",
                searchQuery = "McKinsey company research OR McKinsey Global Institute"
            ),
            AdvisoryFirm(
                id = "bcg",
                name = "Boston Consulting Group (BCG)",
                tag = "BCG Insights",
                description = "BCG Henderson Institute, digital transformation & enterprise tech economics",
                searchQuery = "Boston Consulting Group BCG tech report"
            ),
            AdvisoryFirm(
                id = "idc",
                name = "IDC",
                tag = "MarketScape",
                description = "IDC MarketScape, worldwide IT spending & semiconductors data",
                searchQuery = "IDC MarketScape report OR International Data Corporation"
            )
        )
    }
}
