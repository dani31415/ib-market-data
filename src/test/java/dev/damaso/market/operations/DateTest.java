package dev.damaso.market.operations;

import org.junit.jupiter.api.Assertions;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DateTests {

	/**
	 * 
	 */
	@Test
	void isNasdaqOpen() {
        LocalDateTime d0 = LocalDateTime.parse("2023-05-12T12:54:00");
        LocalDateTime d1 = LocalDateTime.parse("2023-05-12T13:24:00");
        Assertions.assertTrue(Date.isNasdaqOpenDay(d1.toLocalDate()));
        Assertions.assertFalse(Date.isNasdaqOpen(d1));
        LocalDateTime d2 = LocalDateTime.parse("2023-05-12T13:34:00");
        Assertions.assertTrue(Date.isNasdaqOpen(d2));
        LocalDateTime d3 = LocalDateTime.parse("2023-05-12T19:34:00");
        Assertions.assertTrue(Date.isNasdaqOpen(d3));
        LocalDateTime d4 = LocalDateTime.parse("2023-05-12T20:02:00");
        Assertions.assertFalse(Date.isNasdaqOpen(d4));
        LocalDateTime e1 = LocalDateTime.parse("2023-05-13T19:34:00"); // saturday
        Assertions.assertFalse(Date.isNasdaqOpen(e1));
        LocalDateTime f1 = LocalDateTime.parse("2023-05-15T13:38:00"); // monday
        Assertions.assertTrue(Date.isNasdaqOpen(f1));
        LocalDateTime g1 = LocalDateTime.parse("2023-05-16T13:38:00"); // tuesday
        Assertions.assertTrue(Date.isNasdaqOpen(g1));

        Assertions.assertEquals(Date.minutesBetween(d0, d1), 24);
        Assertions.assertEquals(Date.minutesBetween(d1, d2), 10);
        Assertions.assertEquals(Date.minutesBetween(d0, d2), 34);
        Assertions.assertEquals(Date.minutesBetween(d0, d3), 34+6*60);
        Assertions.assertEquals(Date.minutesBetween(d2, d3), 6*60);
        Assertions.assertEquals(Date.minutesBetween(d3, d4), 26);
        Assertions.assertEquals(Date.minutesBetween(d3, e1), 26);
        Assertions.assertEquals(Date.minutesBetween(d3, f1), 64);
        Assertions.assertEquals(Date.minutesBetween(d3, g1), 64+420);

        LocalDateTime d0_1 = LocalDateTime.parse("2023-05-12T13:00:00");
        Assertions.assertEquals(Date.getPreOpen(d1), d0_1);
        Assertions.assertEquals(Date.getPreOpen(d0), d0_1);
	}
}
