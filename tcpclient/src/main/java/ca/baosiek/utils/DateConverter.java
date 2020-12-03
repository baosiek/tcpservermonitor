package ca.baosiek.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateConverter {

    public static LocalDateTime convertToLocalDateTime(Date date) {

        LocalDateTime newDate = LocalDateTime.ofInstant(date.toInstant(),
                ZoneId.systemDefault());

        return newDate;
    }

    public static Date convertToDate(LocalDateTime time) {

        ZonedDateTime zdt = time.atZone(ZoneId.systemDefault());
        Date output = Date.from(zdt.toInstant());

        return output;
    }

    public static String localDateTime2String(LocalDateTime ldt) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime ft = LocalDateTime.parse(ldt.toString(), DateTimeFormatter.ISO_DATE_TIME);

        return ldt.toString();

    }
}
