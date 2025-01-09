package dev.damaso.market.operations;

import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class Date {
    static private ZonedDateTime nextClose(ZonedDateTime date) {
        return date.truncatedTo(ChronoUnit.HOURS).withHour(16);
    }

    static private ZonedDateTime nextDayPreOpen(ZonedDateTime date) {
        ZonedDateTime nextDay;
        nextDay = date;
        do {
            nextDay = nextDay.plus(1, ChronoUnit.DAYS);
        } while (!isNasdaqOpenDay(nextDay.toLocalDate()));
        return getPreOpen(nextDay);
    }

    static public LocalDateTime getPreOpen(LocalDateTime date) {
        ZonedDateTime nasdaqTime = toNasdaqZone(date);
        ZonedDateTime nasdaqPreOpen = nasdaqTime.truncatedTo(ChronoUnit.HOURS).withHour(9);
        return toUTCZone(nasdaqPreOpen);
    }

    static private ZonedDateTime getPreOpen(ZonedDateTime date) {
        return date.truncatedTo(ChronoUnit.HOURS).withHour(9);
    }

    static private ZonedDateTime toNasdaqZone(LocalDateTime localDateTime) {
        ZonedDateTime utcDateTime = localDateTime.atZone(ZoneId.of("UTC"));
        ZonedDateTime zonedDateTime = utcDateTime.withZoneSameInstant(ZoneId.of("America/New_York"));
        return zonedDateTime;
    }

    static private LocalDateTime toUTCZone(ZonedDateTime nasdaqDateTime) {
        ZonedDateTime utcDateTime = nasdaqDateTime.withZoneSameInstant(ZoneId.of("UTC"));
        return utcDateTime.toLocalDateTime();
    }

    static public int minutesBetween(LocalDateTime from0, LocalDateTime to0) {
        ZonedDateTime from = toNasdaqZone(from0);
        if (isNasdaqBeforePreOpen(from)) {
            from = getPreOpen(from);
        } else if (isNasdaqAfterClose(from)) {
            from = nextDayPreOpen(from);
        }
        ZonedDateTime to = toNasdaqZone(to0);

        int minutes = 0;
        ZonedDateTime close = nextClose(from);
        while (to.isAfter(close)) {
            minutes += Duration.between(from, close).toMinutes();
            from = nextDayPreOpen(from);
            close = nextClose(from);
        }

        if (to.isAfter(from)) {
            minutes += Duration.between(from, to).toMinutes();
        }
        return minutes;
    }

    static public boolean isNasdaqOpenDay(LocalDate localDate) {
        // https://www.tradinghours.com/markets/nasdaq
        String closedDates [] = {
            "2022-11-24", // Thanksgiving Day
            "2022-12-26",
            "2023-01-02", // new year's day
            "2023-01-16", // birthday of Martin Luther King, Jr
            "2023-02-20", // Washington's birthday
            "2023-04-07", // Good Friday
            "2023-05-29", // Memorial Day
            "2023-06-19", // Juneteenth
            "2023-07-04", // Independence Day
            "2023-09-04", // Labor Day
            "2023-11-23", // Thanksgiving Day
            "2023-12-25", // Christmas

            "2024-01-01", // new year's day
            "2024-01-15", // birthday of Martin Luther King, Jr
            "2024-02-19", // Washington's birthday
            "2024-03-29", // Good Friday
            "2024-05-27", // Memorial Day
            "2024-06-19", // Juneteenth
            "2024-07-04", // Independence Day
            "2024-09-02", // Labor Day
            "2024-11-28", // Thanksgiving Day
            "2024-12-25", // Christmas

            "2025-01-01", // new year's day
            "2025-01-09", // National Day of Mourning for President Jimmy Carter
            "2025-01-20", // birthday of Martin Luther King, Jr
            "2025-02-17", // Washington's birthday
            "2025-04-18", // Good Friday
            "2025-05-26", // Memorial Day
            "2025-06-19", // Juneteenth
            "2025-07-04", // Independence Day
            "2025-09-01", // Labor Day
            "2025-11-27", // Thanksgiving Day
            "2025-12-25", // Christmas
        };
        DayOfWeek dow = localDate.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        for (String closedDateStr : closedDates) {
            LocalDate closedDate = LocalDate.parse(closedDateStr);
            if (localDate.equals(closedDate)) {
                return false;
            }
        }
        return true;
    }

    static public boolean isNasdaqBeforePreOpen(ZonedDateTime zonedDateTime) {
        double hour = 0.0 + zonedDateTime.getHour() +zonedDateTime.getMinute()/60.0;
        return hour < 9;
    }

    static public boolean isNasdaqAfterClose(ZonedDateTime zonedDateTime) {
        double hour = 0.0 + zonedDateTime.getHour() +zonedDateTime.getMinute()/60.0;
        return hour >= 16;
    }

    static public boolean isNasdaqOpen(LocalDateTime localDateTime) {
        // Check day
        LocalDate localDate = localDateTime.toLocalDate();
        if (!isNasdaqOpenDay(localDate)) {
            return false;
        }

        // Check time
        ZonedDateTime zonedDateTime = toNasdaqZone(localDateTime);
        double hour = 0.0 + zonedDateTime.getHour() +zonedDateTime.getMinute()/60.0;
        if (hour>=9.5 && hour<16) { // 9:30 -- 16:00
            return true;
        }

        return false;
    }

    static public boolean isNasdaqExtendedOpen(LocalDateTime localDateTime) {
        // Check day
        LocalDate localDate = localDateTime.toLocalDate();
        if (!isNasdaqOpenDay(localDate)) {
            return false;
        }

        // Check time
        ZonedDateTime zonedDateTime = toNasdaqZone(localDateTime);
        double hour = 0.0 + zonedDateTime.getHour() +zonedDateTime.getMinute()/60.0;
        if (hour>=8 && hour<17) { // 8:00 -- 17:00
            return true;
        }

        return false;
    }

    static public void main(String args []) {
        System.out.println("Hello world");
    }
}
