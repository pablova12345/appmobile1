package bo.gob.gamc.catastro.digid.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object PdfViewer : Screen("pdf_viewer?filePath={filePath}&fileName={fileName}") {
        fun createRoute(filePath: String, fileName: String): String {
            val encodedPath = java.net.URLEncoder.encode(filePath, "UTF-8")
            val encodedName = java.net.URLEncoder.encode(fileName, "UTF-8")
            return "pdf_viewer?filePath=$encodedPath&fileName=$encodedName"
        }
    }
    object Resoluciones : Screen("resoluciones")
    object AIStudy : Screen("ai_study?fileName={fileName}") {
        fun createRoute(fileName: String): String {
            val encodedName = java.net.URLEncoder.encode(fileName, "UTF-8")
            return "ai_study?fileName=$encodedName"
        }
    }
    object Geoextract : Screen("geoextract")
}
