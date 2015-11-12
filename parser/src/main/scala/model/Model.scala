package model

import play.api.libs.json._

object Models {	
	case class Element(quantity: Int, label: String)
	case class Owner(name:String,bankaccount:Int)
	case class Dimension(width:Int,length:Int,height:Int)
	
	case class Jest(name:String, isValid:Boolean, average:Double,owner:Owner,dimensions:Dimension,elements:Seq[Element])

}

object Formatters {
	import Models._

	implicit val elementFormat = Json.format[Element]
	implicit val ownerFormat = Json.format[Owner]
	implicit val dimensionFormat = Json.format[Dimension]
	implicit val jestFormat = Json.format[Jest]

}