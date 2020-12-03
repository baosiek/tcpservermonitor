package ba.baosiek.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
}
