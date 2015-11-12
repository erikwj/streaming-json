/*package utils

import org.joda.time.DateTime
import org.joda.time.format._
import java.util._
import org.joda.time._

object TimeUtil {

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  /**
   * Reformat any date for Natty so the DD/MM/YYYY become MM/DD/YYYY
   */
  def reformatDate(date: String) = {
    val dateRegex = "(\\d{2})[\\/\\-](\\d{2})[\\/\\-](\\d{4})".r
    val newDate = dateRegex.replaceAllIn(date, { found =>
      val day = found.group(1)
      val month = found.group(2)
      val year = found.group(3)
      month + "-" + day + "-" + year
    })
    newDate
  }

  def getExcelDate(date: DateTime) = {
    val DAY_MILLISECONDS = (60 * 60 * 24 * 1000).toFloat
    val fraction = (date.getMillisOfDay / DAY_MILLISECONDS).toDouble

    // start
    val start = DateTime.now.withYear(1900).withDayOfYear(1).withTimeAtStartOfDay()
    val absoluteDay = Days.daysBetween(start, date).getDays()
    val value = fraction + absoluteDay

    val result = value + 2 // Lotus 123 bug
    "<v>" + result.toString + "</v>"
  }


  def ordinal(date: Option[DateTime]) = {
    date match {
      case Some(d) =>  {
        val cal = d.toGregorianCalendar
        val num = cal.get(Calendar.DAY_OF_MONTH)
        val suffix = Array("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")
        val m = num % 100
        val index = if(m > 10 && m < 20){ 0 } else { (m % 10) }
        num.toString + suffix(index)
      }
      case _ => ""
    }
  }


  def md5(s: String) = {
    import java.security.MessageDigest
    MessageDigest.getInstance("MD5").digest(s.getBytes).map("%02X".format(_)).mkString.toLowerCase
  }

}
/**
* Internal class that gives a date range when passing strings.
*
* start date: date format dd-MM-yyyy HH:mm
* end date: date format dd-MM-yyyy HH:mm
* period: one of "lastTenDays", "lastMonth", "thisMonth", "yesterday", "today", "thisWeek", "lastWeek", "thisMonth", "thisYear", "lastYear",
* "thisQuarter", "lastQuarter"
*/
case class RelativePeriod(period: String) {


  def startDate = {

    period match {
      case "tomorrow" => {
        DateTime.now.plusDays(1).withTimeAtStartOfDay()
      }
      case "nextMonth" => {
        DateTime.now.plusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay()
      }
      case "nextWeek" => {
        DateTime.now.withDayOfWeek(1).plusWeeks(1).withTimeAtStartOfDay()
      }
      case "lastTenDays" => {
        DateTime.now.minusDays(10).withTimeAtStartOfDay()
      }

      case "lastMonth" => {
        DateTime.now.minusMonths(1).withDayOfMonth(1).withTimeAtStartOfDay()
      }

      case "thisMonth" => {
        DateTime.now.withDayOfMonth(1).withTimeAtStartOfDay()
      }

      case "yesterday" => {
        DateTime.now.minusDays(1).withTimeAtStartOfDay()

        // TODO: if yesterday is a week end then we ask Friday
        /*if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
        cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        cal.add(Calendar.WEEK_OF_MONTH, -1);
      }*/
    }

    case "today" => {
      DateTime.now.withTimeAtStartOfDay()
    }

    case "thisWeek" => {
      DateTime.now.withDayOfWeek(1).withTimeAtStartOfDay()
    }


    case "lastWeek" => {
      DateTime.now.withDayOfWeek(1).minusWeeks(1).withTimeAtStartOfDay()
    }

    case "thisQuarter" => {
      DateTime.now
      // TODO!
      // A quarter is 3 months. 1st quarter jan-end mars, april - end june, july - end sept, oct - end dec
      /*val month = cal.get(Calendar.MONTH)
      val startMonth = month match {
      case it if 0 until 3 contains it => 0
      case it if 3 until 6 contains it => 3
      case it if 6 until 9 contains it => 6
      case _ => 9
    }
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.MONTH, startMonth)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.getTime*/
  }

  case "lastQuarter" => {
    DateTime.now
    // TODO!
    // A quarter is 3 months. 1st quarter jan-end mars, april - end june, july - end sept, oct - end dec
    /*val month = cal.get(Calendar.MONTH)
    val startMonth = month match {
    case it if 0 until 3 contains it => cal.add(Calendar.YEAR, -1); 9// previous quarter is 10 but year is -1
    case it if 3 until 6 contains it => 0
    case it if 6 until 9 contains it => 3
    case _ => 6
  }
  cal.set(Calendar.DAY_OF_MONTH, 1)
  cal.set(Calendar.MONTH, startMonth)
  cal.set(Calendar.HOUR_OF_DAY, 0)
  cal.set(Calendar.MINUTE, 0)
  cal.set(Calendar.SECOND, 0)
  cal.set(Calendar.MILLISECOND, 0)
  cal.getTime*/
}

case "thisYear" => {
  DateTime.now.withDayOfYear(1).withTimeAtStartOfDay()
}

case "lastYear" => {
  DateTime.now.minusYears(1).withDayOfYear(1).withTimeAtStartOfDay()
}

case _ => {
  // Do we have a year? YYYY
  try {
    DateTimeFormat.forPattern("YYYY").parseDateTime(period).withDayOfYear(1)
  } catch {
    case e : Throwable => {
      // Do we have 2 dates? ie 2011-02-02T01:00/2012-02-02T01:00
      val p = period.split("/")
      DateTime.parse(p(0))
    }
  }
  //DateTime.now.withTimeAtStartOfDay()

}
}
}


def endDate = period match {

  case "tomorrow" => {
    DateTime.now.plusDays(2).withTimeAtStartOfDay().minusSeconds(1)
  }

  case "nextMonth" => {
    DateTime.now.plusMonths(1).dayOfMonth().withMaximumValue().withTime(23, 59, 59, 999)
  }

  case "nextWeek" => {
    DateTime.now.plusWeeks(1).withDayOfWeek(7).withTime(23, 59, 59, 999)
  }

  case "lastTenDays" => {
    DateTime.now.withTimeAtStartOfDay()
  }

  case "lastMonth" => {
    DateTime.now.minusMonths(1).dayOfMonth().withMaximumValue()
  }

  case "thisMonth" => {
    DateTime.now.dayOfMonth().withMaximumValue()
  }

  case "today" => {
    DateTime.now.plusDays(1).withTimeAtStartOfDay().minusSeconds(1)
  }

  case "yesterday" => {
    DateTime.now.minusDays(1).withTimeAtStartOfDay().plusHours(18)
  }

  case "thisWeek" => {
    DateTime.now.withDayOfWeek(7).withTimeAtStartOfDay()
  }

  case "lastWeek" => {
    DateTime.now.minusWeeks(1).withDayOfWeek(7).withTimeAtStartOfDay()
  }

  case "thisQuarter" => {
    DateTime.now
    // TODO
    // A quarter is 3 months. 1st quarter jan-end mars, april - end june, july - end sept, oct - end dec
    /*val month = cal.get(Calendar.MONTH)
    val startMonth = month match {
    case it if 0 until 3 contains it => 4
    case it if 3 until 6 contains it => 7
    case it if 6 until 9 contains it => 9
    case _ => 0
  }
  cal.set(Calendar.DAY_OF_MONTH, 1)
  cal.set(Calendar.MONTH, startMonth)
  cal.set(Calendar.HOUR_OF_DAY, 0)
  cal.set(Calendar.MINUTE, 0)
  cal.set(Calendar.SECOND, 0)
  cal.set(Calendar.MILLISECOND, 0)

  cal.add(Calendar.DAY_OF_MONTH, -1)

  cal.getTime*/
}

case "lastQuarter" => {
  DateTime.now
  // TODO
  // A quarter is 3 months. 1st quarter jan-end mars, april - end june, july - end sept, oct - end dec
  /*val month = cal.get(Calendar.MONTH)
  val startMonth = month match {
  case it if 0 until 3 contains it => cal.add(Calendar.YEAR, -1); 0// previous quarter is 10 but year is -1
  case it if 3 until 6 contains it => 3
  case it if 6 until 9 contains it => 7
  case _ =>9
}
cal.set(Calendar.DAY_OF_MONTH, 1)
cal.set(Calendar.MONTH, startMonth)
cal.set(Calendar.HOUR_OF_DAY, 0)
cal.set(Calendar.MINUTE, 0)
cal.set(Calendar.SECOND, 0)
cal.set(Calendar.MILLISECOND, 0)
cal.add(Calendar.DAY_OF_MONTH, -1)

cal.getTime*/
}

case "lastYear" => {
  DateTime.now.minusYears(1).withMonthOfYear(12).withDayOfMonth(31).withTime(23, 59, 59, 999)
}

case "thisYear" => {
  DateTime.now.withMonthOfYear(12).withDayOfMonth(31).withTime(23, 59, 59, 999)

}

case _ => {
  try {
    DateTimeFormat.forPattern("YYYY").parseDateTime(period).withDayOfMonth(31).withMonthOfYear(12).withTime(23, 59, 59, 999)
  } catch {
    case e : Throwable => {
      // Do we have 2 dates? ie 2011-02-02T01:00/2012-02-02T01:00
      val p = period.split("/")
      DateTime.parse(p(1))

    }
  }
  //DateTime.now
}
}
}
*/