package com.example.data.model

data class WorkAccount(
    val id: String,
    val name: String,
    val tickerOrDomain: String = "",
    val industry: String = "Enterprise Tech",
    val isSelected: Boolean = true
) {
    companion object {
        val DEFAULT_WORK_ACCOUNTS = listOf(
            WorkAccount("acc_msft", "Microsoft", "MSFT", "Cloud & AI Enterprise"),
            WorkAccount("acc_goog", "Google / Alphabet", "GOOGL", "Cloud & Search"),
            WorkAccount("acc_amzn", "Amazon Web Services (AWS)", "AMZN", "Cloud Infrastructure"),
            WorkAccount("acc_nvda", "NVIDIA", "NVDA", "AI Hardware & Semiconductors"),
            WorkAccount("acc_crm", "Salesforce", "CRM", "Enterprise SaaS & Agentforce"),
            WorkAccount("acc_orcl", "Oracle", "ORCL", "Database & Cloud Infrastructure"),
            WorkAccount("acc_ibm", "IBM", "IBM", "Hybrid Cloud & Consulting"),
            WorkAccount("acc_snow", "Snowflake", "SNOW", "Data Cloud & Analytics"),
            WorkAccount("acc_adbe", "Adobe", "ADBE", "Creative & Enterprise Marketing"),
            WorkAccount("acc_infy", "Infosys", "INFY", "IT Services & Consulting"),
            WorkAccount("acc_tcs", "Tata Consultancy Services (TCS)", "TCS", "IT & Transformation"),
            WorkAccount("acc_wipro", "Wipro", "WIT", "Global Tech Services")
        )
    }
}
