package dev.minemods.thermal;
import org.junit.jupiter.api.Test;
final class ThermalModelRegressionTest {
    @Test void allThermalAssertions() {
        ThermalModelTest.checks = 0;
        ThermalModelTest.main(new String[0]);
        org.junit.jupiter.api.Assertions.assertEquals(27, ThermalModelTest.checks);
    }
}
