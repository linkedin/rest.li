package com.linkedin.data.codec;

import com.linkedin.data.DataComplex;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.symbol.InMemorySymbolTable;
import com.linkedin.data.codec.symbol.SymbolTable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public class Benchmarks {

  @State(Scope.Thread)
  public static abstract class AbstractBenchmark {

    DataCodec _codec;
    DataMap _case1;
    DataMap _case2;
    DataMap _case3;

    @Setup(Level.Iteration)
    public void setup() {
      Map<String, DataComplex> fixtures = CodecDataProviders.codecDataInputs();
      _case1 = (DataMap) fixtures.get("Reference DataMap1");
      _case2 = (DataMap) fixtures.get("Map of variable length strings");
      _case3 = (DataMap) fixtures.get("Map containing list of 100 20-byte bytes");

      // allow getCodec access to the newly populated fixtures
      _codec = getCodec();
    }

    @Benchmark
    public byte[] basicMixedDataMap() throws IOException {
      return _codec.mapToBytes(_case1);
    }

    @Benchmark
    public byte[] mapOfVariableLengthStrings() throws IOException {
      return _codec.mapToBytes(_case2);
    }

    @Benchmark
    public byte[] mapOfListOfManySmallByteStrings() throws IOException {
      return _codec.mapToBytes(_case3);
    }

    protected abstract DataCodec getCodec();
  }

  public static class BsonDataCodecBenchmark extends AbstractBenchmark {
    @Override
    protected DataCodec getCodec() {
      return new BsonDataCodec();
    }
  }

  public static class JacksonDataCodecBenchmark extends AbstractBenchmark {
    @Override
    protected DataCodec getCodec() {
      return new JacksonDataCodec();
    }
  }

  public static class JacksonLICORDataCodecBenchmark extends AbstractBenchmark {
    @Override
    protected DataCodec getCodec() {

      Set<String> symbols = new HashSet<>();
      TestCodec.collectSymbols(symbols, _case1);
      TestCodec.collectSymbols(symbols, _case2);
      TestCodec.collectSymbols(symbols, _case3);

      SymbolTable symbolTable = new InMemorySymbolTable(new ArrayList<>(symbols));

      final String sharedSymbolTableName = "SHARED";
      JacksonLICORDataCodec.setSymbolTableProvider(symbolTableName -> {
        if (sharedSymbolTableName.equals(symbolTableName))
        {
          return symbolTable;
        }

        return null;
      });

      return new JacksonLICORDataCodec(true, sharedSymbolTableName);
    }
  }
}
