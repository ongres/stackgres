package io.stackgres.apiweb.transformer.dbops.converter;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusTransactionsPerSecond;
import io.stackgres.apiweb.transformer.dbops.converter.fixture.StackGresDbOpsPgbenchStatusTransactionsPerSecondFixture;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusTransactionsPerSecond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DbOpsPgbenchStatusTransactionsPerSecondConverterTest {

  private DbOpsPgbenchStatusTransactionsPerSecondConverter converter;
  private StackGresDbOpsPgbenchStatusTransactionsPerSecond source;

  @BeforeEach
  public void setup() {
    this.converter = new DbOpsPgbenchStatusTransactionsPerSecondConverter();
    this.source = new StackGresDbOpsPgbenchStatusTransactionsPerSecondFixture().build();
  }

  @Test
  void shouldReturnNull_whenSGDbOpsPgbenchStatusStandartDeviationIsNull() {
    assertNull(converter.from(null));
  }

  @Test
  void shouldConvertfromSGDbOpsPgbenchTransactionPerSecond_toDbOpsPgbenchStatusTransactionsPerSecond() {
    DbOpsPgbenchStatusTransactionsPerSecond dto = converter.from(source);
    
    assertEquals(dto.getExcludingConnectionsEstablishing().getValue(),
        source.getExcludingConnectionsEstablishing().getValue());
    assertEquals(dto.getExcludingConnectionsEstablishing().getUnit(),
        source.getExcludingConnectionsEstablishing().getUnit());
    assertEquals(dto.getIncludingConnectionsEstablishing().getValue(),
        source.getIncludingConnectionsEstablishing().getValue());
    assertEquals(dto.getIncludingConnectionsEstablishing().getUnit(),
        source.getIncludingConnectionsEstablishing().getUnit());
  }

}
