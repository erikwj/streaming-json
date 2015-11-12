package parser;

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson._
import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonMappingException

import org.apache.poi.ss.usermodel.Cell

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Font;

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.streaming.SXSSFWorkbook

import java.io.File;
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.OutputStream
import java.io.InputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream

import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.collection.mutable.{ Buffer, ListBuffer }
import scala.language.higherKinds
import scala.util.{Success, Failure}

import scalaz._
import Scalaz._

import play.api.libs.json._
import play.api.libs.iteratee.{Enumeratee, Enumerator, Iteratee,Input,Done}


object ParseJsonSampleScala {

import ShipmentJsonParser._

  val jFactory: JsonFactory = new MappingJsonFactory()

  def toIntOpt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case e: Exception => None
    }
  }

  case class ColumnId(index: Int) extends AnyVal
  
  def main(args: Array[String]): Unit = {
    import ExcelWriter._
    
    val workbook = new SXSSFWorkbook(10)

    // val sh = workbook.createSheet()
    implicit val styles = new ExcelWriter(workbook)

    val f: JsonFactory = new MappingJsonFactory()
    val dataFile: String = "resources/maxrows.json"
    val jFile = new File(dataFile)

    val defaultColSeq = (1 to 6).toSeq
    val columnsSeq: Seq[ColumnId] = (0 +: defaultColSeq).map((s) => ColumnId(s))
    
/*    val report = new ExcelReport(jFile,workbook,columnsSeq)
    val report = new ExcelReport(jFile,workbook,columnsSeq,styles)
    report.run*/
    
    toExcel(jFile, workbook, columnsSeq)
    


  }

  def parseShipmentToSheet(jp: JsonParser, workbook: SXSSFWorkbook,columns: Seq[ColumnId]) = {

    import ExcelWriter._

    val sh = workbook.createSheet()
    implicit val styles = new ExcelWriter(workbook)

    val columnParser = Map(
      ColumnId(0) ->  StringColumn("Name",new StringCell(label)) ,  
      ColumnId(1) ->  StringColumn("owner name",new StringCell(ownerName)) ,
      ColumnId(2) ->  IntColumn("owner bankaccount",new IntCell(ownerAccount)) ,
      ColumnId(3) ->  IntColumn("width", new IntCell(width)) ,
      ColumnId(4) ->  IntColumn("length", new IntCell(length)) ,
      ColumnId(5) ->  IntColumn("height", new IntCell(height)) ,
      ColumnId(6) ->  StringColumn("Valid",new StringCell(isValid)) 
      // ColumnId(7) ->  StringColumn("Total Elements",new StringCell(incoTerm)) ,
      // ColumnId(8) ->  DateColumn("Average",new DateCell(eta))
      )

    var rownum = 0
    def currentToken: JsonToken = jp.nextToken

    if (currentToken != JsonToken.START_OBJECT) { println("Error: root should be object: quiting.") }
    val headerRow = sh.createRow(rownum)
    val headerCell = ExcelWriter.cellWithStyle(headerRow, _: Int, _: String, styles.headerCellStyle)
   

    val selectedColumns:Seq[Column] = (columns map { (ci) => columnParser.get(ci) }).flatten
    
    val selected = selectedColumns.zipWithIndex 

    selected map { (col_index) => {
      val col = col_index._1
      val index = col_index._2
      val header = col.header
      headerCell(index,header) 
      }
    }
    

    while (currentToken != JsonToken.END_OBJECT) {
      val fieldName = jp.getCurrentName();
      if (fieldName.equals("testjsons")) {
        if (currentToken == JsonToken.START_ARRAY) {
          // For each of the records in the array
          while (jp.nextToken() != JsonToken.END_ARRAY) {

            // read the record into a tree model,
            // this moves the parsing position to the end of it
            val node: JsonNode = jp.readValueAsTree();
            // And now we have random access to everything in the object
            rownum += 1
            val row = sh.createRow(rownum)

            selected map { (col_index) => {
              val col = col_index._1
              val index = col_index._2
              col.write(row,index,node) 
              }
            }
            
          }
        } else {
          // println("Error: records should be an array: skipping.");
        }
      } else {
        // println("Unprocessed property: " + fieldName);
        jp.skipChildren();
      }
    }
  }

  def toExcel(tempFile: File, wb: SXSSFWorkbook, cols: Seq[ColumnId]): String = {

    val inputStream: InputStream = new FileInputStream(tempFile)
    //Should be more robust
    val reportName = (tempFile.getCanonicalPath).replaceAll("\\.json","\\.xlsx")
    // val reportName = "temp/report" + "-" + new DateTime().toString("dd-MM-yyyy") + ".xlsx"
    val out = new FileOutputStream(reportName)

    val jp: JsonParser = jFactory.createParser(inputStream)

    parseShipmentToSheet(jp, wb,cols)

    wb.write(out)

    // Close resources
    wb.close()
    wb.dispose()
    jp.close()
    inputStream.close()
    tempFile.delete()
    out.close()
    reportName
  }


}


object ShipmentJsonParser {

  val formatFromJson = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH)
  val defaultDateFormatExcel = new SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.ENGLISH)
  val shortDateFormatExcel = new SimpleDateFormat("EEE dd MMM. yyyy ", Locale.ENGLISH)


  val label = (jnode:JsonNode) => (jnode |> get("name") >>= getText).getOrElse("")
  val ownerName = (jnode:JsonNode) => (jnode |> get("owner") >>= get("name") >>= getText).getOrElse("")
  val ownerAccount = (jnode:JsonNode) => (jnode |> get("owner") >>= get("bankaccount") >>= getInt).getOrElse(0)
  val width = (jnode:JsonNode) => (jnode |> get("dimensions") >>= get("width") >>= getInt).getOrElse(0)
  val length = (jnode:JsonNode) => (jnode |> get("dimensions") >>= get("length") >>= getInt).getOrElse(0)
  val height = (jnode:JsonNode) => (jnode |> get("dimensions") >>= get("height") >>= getInt).getOrElse(0)
  val isValid = (jnode:JsonNode) => (jnode |> get("isValid") >>= getBoolean).getOrElse(false) |> boolText
  
  // val totalQuantities = (jnode:JsonNode) => {
  //   val elements: Iterator[JsonNode] = (jnode |> get("elements") map getElements).getOrElse(Iterator[JsonNode]())
  //   val parcelQuantities: Int = elements map { (it) => foldit(it)(getIntField("quantity")) }
  //   parcelQuantities.sum
  // }

/*
  val preShipArea = (jnode:JsonNode) => (jnode |> get("pre_ship_area") >>= get("text") >>= getText).getOrElse("")
  val countryName = (jnode:JsonNode) => (jnode |> get("destination") >>= get("country") >>= get("name") >>= getText).getOrElse("")
  val postalCodeDest = (jnode:JsonNode) => (jnode |> get("destination") >>= get("address") >>= get("post_code") >>= getText).getOrElse("")
  val incoTerm = (jnode:JsonNode) => {
    val inco_code = (jnode |> get("incoterm") >>= get("code") >>= getText).getOrElse("")
    val inco_loc = (jnode |> get("incoterm") >>= get("location") >>= getText).getOrElse("")
    
      if (inco_code.nonEmpty && inco_loc.nonEmpty) inco_code + "-" + inco_loc
      else ""
  }

  val eta = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("eta") >>= getDate).getOrElse("")
  val startDate = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("start_date") >>= getDate).getOrElse("")
  val orderPreparedDate = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("order_prepared_date") >>= getDate).getOrElse("")
  val shipmentRequestDate = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("shipment") >>= get("shipment_request") >>= getText).getOrElse("")
  val shipmentDate = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("ship_date") >>= getDate).getOrElse("")
  val calculatedStatusResult = (jnode:JsonNode) => (jnode |> get("references") >>= get("calculated_status_result") >>= getText).getOrElse("")
  val statusFaultExplanation = (jnode:JsonNode) => (jnode |> get("references") >>= get("status_fault_explanation") >>= getText).getOrElse("")
  val promisedDate = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("promised_date") >>= getDate).getOrElse("")
  val hanMovEta = (jnode:JsonNode) => (jnode |> get("references") >>= get("hanmov_eta") >>= getDate).getOrElse("")
  val shipReqEta = (jnode:JsonNode) => (jnode |> get("references") >>= get("shipreq_eta") >>= getDate).getOrElse("")
  val carrierName = (jnode:JsonNode) => (jnode |> get("carrier") >>= get("name") >>= getText).getOrElse("")
  val carrierEta = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("carrier_eta") >>= getDate).getOrElse("")
  val carrierPod = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("carrier_pod") >>= getDate).getOrElse("")
  val brokerName = (jnode:JsonNode) => (jnode |> get("broker_name") >>= getText).getOrElse("")
  val brokerEta = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("broker_eta") >>= getDate).getOrElse("")
  val brokerPod = (jnode:JsonNode) => (jnode |> get("timeline") >>= get("broker_pod") >>= getText).getOrElse("")
  val shipmentWeight = (jnode:JsonNode) => (jnode |> get("dimensions") >>= get("weight") >>= getDouble).getOrElse(0d)
  val shipmentVolume = (jnode:JsonNode) => (jnode |> get("dimensions") >>= get("volume") >>= getDouble).getOrElse(0d)
  val shipmentNumOfPieces = (jnode:JsonNode) => (jnode |> get("dimensions") >>= get("total_pieces") >>= getDouble).getOrElse(0d)
  val shipmentNumOfParcels = (jnode:JsonNode) => {
    val parcels: Iterator[JsonNode] = (jnode |> get("parcels") map getElements).getOrElse(Iterator[JsonNode]())
    val parcelLinesOpt: Seq[Option[JsonNode]] = mapit(parcels)(get("parcel_lines") _)
    val parcelQuantities: Seq[Int] = parcelLinesOpt.flatten map getElements map { (it) => foldit(it)(getIntField("quantity")) }
    parcelQuantities.sum
  }

  val receptionDate = (jnode:JsonNode) => (jnode |> get("invoice_document") >>= get("reception_date") >>= getDate).getOrElse("")
  val invoicePrintDate = (jnode:JsonNode) => (jnode |> get("invoice_document") >>= get("print_date") >>= getDate).getOrElse("")
  val customsDossierNumber = (jnode:JsonNode) => (jnode |> get("references") >>= get("customs_dossier_number") >>= getText).getOrElse("").nonEmpty |> boolText
  val exportLicenseText = (jnode:JsonNode) => (jnode |> get("transport_details") >>= get("export_license_text") >>= getText).getOrElse("").nonEmpty |> boolText
  val journeyStatus = (jnode:JsonNode) => {
    val history: Iterator[JsonNode] = (jnode |> get("status_history") map getElements).getOrElse(Iterator[JsonNode]())
    val lastHistoryObject = history.toSeq.reverse
    if (lastHistoryObject.size > 0) {
      val date = (lastHistoryObject.head |> get("datetime") >>= getDateShort).getOrElse("")
      (lastHistoryObject.head |> get("text") >>= getText).getOrElse("") + " - " + date
    } else ""
  }*/

  def get(field: String)(json: JsonNode): Option[JsonNode] = Option(json.get(field))

  def getField(field: String): JsonNode => Option[String] = (json: JsonNode) => {
    val nodeOpt = get(field)(json)
    nodeOpt map (_.textValue)
  }

  def getIntField(field: String): JsonNode => Option[Int] = (json: JsonNode) => {
    val nodeOpt = get(field)(json)
    nodeOpt map (_.asInt)
  }

  val getBoolean: JsonNode => Option[Boolean] = (json: JsonNode) => Option(json.booleanValue)

  val getText: JsonNode => Option[String] = (json: JsonNode) => Option(json.textValue)

  val getTextOpt: Option[JsonNode] => Option[String] = (json: Option[JsonNode]) => json map ((json) => json.textValue)

  val getDate: JsonNode => Option[String] = (json: JsonNode) => Option(json.textValue) //map { (datestring: String) => defaultDateFormatExcel.format(formatFromJson.parse(datestring)) }

  val getDateShort: JsonNode => Option[String] = (json: JsonNode) => Option(json.textValue) map { (datestring: String) => shortDateFormatExcel.format(formatFromJson.parse(datestring)) }

  val getDouble: JsonNode => Option[Double] = (json: JsonNode) => Some(json.asDouble)

  val getInt: JsonNode => Option[Int] = (json: JsonNode) => Some(json.asInt)

  val getElements: JsonNode => Iterator[JsonNode] = (json: JsonNode) => json.elements

  def mapit[A, B](iterator: Iterator[A])(f: A => B): Seq[B] = {
    val buffer: ListBuffer[B] = ListBuffer()
    while (iterator.hasNext) {
      buffer.append(f(iterator.next))
    }
    buffer.toSeq
  }

  def foldit[T](iterator: Iterator[JsonNode])(f: JsonNode => Option[T])(implicit monoid: Monoid[T]): T = {
    val tobeFolded: Seq[T] = (mapit(iterator)(f)) map { (tOpt) => tOpt.getOrElse(monoid.zero) }
    tobeFolded.foldLeft(monoid.zero)((a: T, b: T) => monoid.append(a, b))
  }

  val boolText: Boolean => String = (b: Boolean) => if (b) "Yes" else "No"
}


class ExcelWriter(val workbook: SXSSFWorkbook) {

  /*
   * Styles
   */
  def font = workbook.createFont()
  def cellStyle = workbook.createCellStyle()

  def fontStyleBold = {
    val f = font
    f.setBoldweight(Font.BOLDWEIGHT_BOLD)
    f.setFontHeightInPoints(10.toShort)
    f
  }

  def fontStyleNormal = {
    val f = workbook.createFont()
    f.setFontHeightInPoints(10.toShort)
    f
  }

  def fontStyleHeader = {
    val f = font
    f.setBoldweight(Font.BOLDWEIGHT_BOLD)
    f.setFontHeightInPoints(28.toShort)
    f
  }

  val normalFont: Font = fontStyleNormal
  val boldFont: Font = fontStyleBold
  val headerFont: Font = fontStyleHeader

  def cellStyleBold = {
    val cs = cellStyle
    cs.setBorderBottom(CellStyle.BORDER_THIN)
    cs.setBottomBorderColor(IndexedColors.BLACK.getIndex())
    cs.setFont(boldFont)
    cs
  }

  def cellStyleHeader = {
    val cs = cellStyle
    cs.setBorderBottom(CellStyle.BORDER_THICK)
    cs.setBottomBorderColor(IndexedColors.BLACK.getIndex())
    cs.setFont(headerFont)
    cs
  }
  def cellStyleBody = {
    val cs = cellStyle
    cs.setFont(normalFont)
    cs
  }

  def cellStyleDate = {
    val cs = cellStyle
    cs.setFont(normalFont)
    cs.setDataFormat(workbook.createDataFormat.getFormat("m/d/yy h:mm"))
    cs
  }

  val boldCellStyle = cellStyleBold
  val headerCellStyle = cellStyleHeader
  val bodyCellStyle = cellStyleBody
  val dateCellStyle = cellStyleBody

}

object ExcelWriter {
  
  def cellWithStyle(row: Row, col: Int, value: String, style: CellStyle) = {
  val cell = row.createCell(col)
  cell.setCellValue(value)
  cell.setCellStyle(style)
  }

  def cellWithStyle(row: Row, col: Int, value: Date, style: CellStyle) = {
  val cell = row.createCell(col)
  cell.setCellValue(value)
  cell.setCellStyle(style)
  }

  def cellWithStyle(row: Row, col: Int, value: Double, style: CellStyle) = {
  val cell = row.createCell(col)
  cell.setCellValue(value)
  cell.setCellStyle(style)
  }

  trait ExcelCell[T] {
    val wb: ExcelWriter
    val style: CellStyle
    val content: JsonNode => T
    val write: (Row,Int,JsonNode) => Unit// = (row:Row,col:Int, json: JsonNode) => wb.cellWithStyle(row, col, content(json), style)
  }

  class StringCell(val content: JsonNode => String)(implicit sheet: ExcelWriter) extends ExcelCell[String] {
    val wb = sheet
    val style: CellStyle = wb.bodyCellStyle
    val write: (Row,Int,JsonNode) => Unit = (row:Row,col:Int,json:JsonNode) => cellWithStyle(row, col, content(json), style)          
  }

  class IntCell(val content: JsonNode => Int)(implicit sheet: ExcelWriter) extends ExcelCell[Int] {
    val wb: ExcelWriter = sheet
    val style: CellStyle = wb.bodyCellStyle
    val write: (Row,Int,JsonNode) => Unit = (row:Row,col:Int,json:JsonNode) => cellWithStyle(row, col, content(json), style)
  }

  class DateCell(val content: JsonNode => String)(implicit sheet: ExcelWriter) extends ExcelCell[String] {
    val wb: ExcelWriter = sheet
    val style: CellStyle = wb.dateCellStyle
    val write: (Row,Int,JsonNode) => Unit = (row:Row,col:Int,json:JsonNode) => cellWithStyle(row, col, content(json), style)
  }

  class DoubleCell(val content: JsonNode => Double)(implicit sheet: ExcelWriter) extends ExcelCell[Double] {
    val wb: ExcelWriter = sheet
    val style: CellStyle = wb.bodyCellStyle
    val write: (Row,Int,JsonNode) => Unit = (row:Row,col:Int,json:JsonNode) => cellWithStyle(row, col, content(json), style)
  }

  sealed trait Column {
    def header: String
    val write: (Row,Int,JsonNode) => Unit
  }

  sealed trait ExcelColumn[T] extends Column  {
    def header: String
    def data: ExcelCell[T]
  } 
  
  case class StringColumn(header:String, data: ExcelCell[String]) extends ExcelColumn[String] {
    val write = data.write
  }
  
  case class DateColumn(header:String, data: ExcelCell[String]) extends ExcelColumn[String]{
    val write = data.write
  }
  
  case class IntColumn(header:String, data: ExcelCell[Int]) extends ExcelColumn[Int]{
    val write = data.write
  }

  case class DoubleColumn(header:String, data: ExcelCell[Double]) extends ExcelColumn[Double]{
    val write = data.write
  }


}













