package rahmat.rmdn.generatebearer

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.apache.poi.util.IOUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import javax.servlet.http.HttpServletResponse

@SpringBootApplication
class GenerateBearerApplication

fun main(args: Array<String>) {
	runApplication<GenerateBearerApplication>(*args)
}

@RestController
internal class EmployeeController() {

	@PostMapping("/bulk-bearer")
	fun bulkBearer(@RequestBody users: List<User?>, httpServletResponse: HttpServletResponse): ByteArray {
		val data = mutableListOf<MutableMap<String, String?>>()
		users.forEach { it ->
			val item = mutableMapOf<String, String?>()
			item["username"] = it?.username
			item["password"] = it?.password

			val token = requestBearer(it?.password, it?.username)
			item["bearer"] = token

			data.add(item)
		}
		return writeToExcelFile(data,httpServletResponse, "excelnya")
	}
}

fun requestBearer(pass:String?, username: String?): String? {
	try {
		val restTemplate = RestTemplate()

		val url = "http://localhost:8080/sso"
		val requestJson = "{\"username\":\"$username\", \"password\":\"$pass\"}"
		val headers = HttpHeaders()
		headers.contentType = MediaType.APPLICATION_JSON
		val entity = HttpEntity(requestJson, headers)
		val response = restTemplate.postForObject(url, entity, ResponseGetBulkBearer::class.java)

		if (response?.success == true) {
			return response.data?.accessToken
		}
		return null
	} catch (e: Exception) {
		e.printStackTrace()
		return null
	}
}

fun writeToExcelFile(
	data: MutableList<MutableMap<String, String?>>, httpServletResponse: HttpServletResponse, fileName: String
): ByteArray {
	val xlWb = XSSFWorkbook()
	val xlWs = xlWb.createSheet()

	// Create headers
	val keys = data.first().keys
	val headerRow = xlWs.createRow(0)
	keys.forEachIndexed { index, key ->
		headerRow.createCell(index).setCellValue(key)
	}

	// Insert data
	data.forEachIndexed { index, data ->
		val row = xlWs.createRow(index + 1)
		keys.forEachIndexed { columnIndex, s ->
			println(data[s].toString())
			row.createCell(columnIndex).setCellValue(data[s].toString())
		}
	}

	val dir = File(System.getProperty("java.io.tmpdir") + getTimeDir())
	if (!dir.exists()) {
		dir.mkdirs()
	}

	val filepath = dir.absolutePath + File.separator + fileName + System.currentTimeMillis() + ".xlsx"

	val outputStream = FileOutputStream(filepath)
	xlWb.write(outputStream)

	val outputFile = File(filepath)

	httpServletResponse.setHeader("Content-Disposition", "attachment; filename=" + outputFile.name)
	val fis = FileInputStream(outputFile)
	return IOUtils.toByteArray(fis)
}

fun getTimeDir(): String {
	return try {
		val cal = Calendar.getInstance()
		cal.time = Date()
		cal.get(Calendar.YEAR)
			.toString() + File.separator + (cal.get(Calendar.MONTH) + 1).toString() + File.separator + cal.get(
			Calendar.DAY_OF_MONTH
		).toString() + File.separator
	} catch (ex: Exception) {
		""
	}
}


// entity
data class User(
	val username: String?,
	val password: String?
)

class ResponseGetBulkBearer {
	val success: Boolean? = null
	val message: String? = null
	val data: Data? = null
}

data class Data(
	@JsonProperty("access_token")
	val accessToken: String?
)

