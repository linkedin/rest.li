package com.linkedin.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Predicate;


/**
 * Wraps a {@link Writer} and tracks current line and column numbers
 */
public final class LineColumnNumberWriter extends Writer
{

  private final Writer _writer;
  private final Stack<CharacterPosition> _savedPositionStack = new Stack<>();
  private int _column;
  private int _line;
  private int _previousChar;
  private Predicate<Character> _isWhitespaceFunction;
  private final CharacterPosition _lastNonWhitespacePosition;

  /**
   * Creates a new writer.
   *
   * @param out a Writer object to provide the underlying stream.
   */
  public LineColumnNumberWriter(Writer out)
  {
    _writer = out;
    _column = 1;
    _line = 1;
    _previousChar = -1;
    _isWhitespaceFunction = (Character::isWhitespace);
    _lastNonWhitespacePosition = new CharacterPosition(0, 0);
  }

  /**
   * Returns 1 based indices of row and column next character will be written to
   */
  public CharacterPosition getCurrentPosition()
  {
    return new CharacterPosition(_line, _column);
  }

  /**
   * Returns 1 based indices of last row and column ignoring trailing whitespace characters
   */
  public CharacterPosition getLastNonWhitespacePosition()
  {
    return _lastNonWhitespacePosition;
  }

  /**
   * Saves current row and column to be retrieved later by calling {@link #popSavedPosition()}
   *
   * Saved positions are stored in a stack so that calls to saveCurrentPosition() and
   * {@link #popSavedPosition()} can be nested. Saved positions are adjusted to skip whitespace to make it
   * easier to get actual token start positions in indented output. If you call saveCurrentPosition() at column x
   * and then write four spaces followed by non-whitespace, the column number returned by
   * {@link #popSavedPosition()} will be x + 4.
   */
  public void saveCurrentPosition()
  {
    _savedPositionStack.push(new CharacterPosition(_line, _column));
  }

  /**
   * Retrieves row and column from the last time {@link #saveCurrentPosition()} was called
   */
  public CharacterPosition popSavedPosition()
  {
    return _savedPositionStack.pop();
  }

  /**
   * Override definition of whitespace used to adjust character positions to skip
   * whitespace. By default, the definition of whitespace is provided by {@link java.lang.Character#isWhitespace}
   */
  public void setIsWhitespaceFunction(Predicate<Character> isWhitespaceFunction)
  {
    _isWhitespaceFunction = isWhitespaceFunction;
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException
  {
    _writer.write(cbuf, off, len);
    for (; len > 0; len--)
    {
      char c = cbuf[off++];
      int lastLine = _line;
      int lastColumn = _column;
      updateCurrentPosition(c);
      _previousChar = c;
      if (_isWhitespaceFunction.test(c))
      {
        updateSavedPositionsForWhitespace(lastLine, lastColumn);
      } else
      {
        _lastNonWhitespacePosition.line = lastLine;
        _lastNonWhitespacePosition.column = lastColumn;
      }
    }
  }

  @Override
  public void flush() throws IOException
  {
    _writer.flush();
  }

  @Override
  public void close() throws IOException
  {
    _writer.close();
  }

  @Override
  public String toString()
  {
    return _writer.toString();
  }

  private void updateCurrentPosition(char c)
  {
    if (_previousChar == '\r')
    {
      if (c == '\n')
      {
        _column = 1;
      } else
      {
        _column = 2;
      }
    } else if (c == '\n' || c == '\r')
    {
      _column = 1;
      ++_line;
    } else
    {
      ++_column;
    }
  }

  /**
   * Any saved positions that are equal to the current row and column are set to the current position in order to
   * remove leading whitespace. Once the first non-whitespace character is written, the current position will be
   * different from any saved positions and the current position will advance.
   */
  private void updateSavedPositionsForWhitespace(int lastLine, int lastColumn)
  {
    for (int i = _savedPositionStack.size() - 1; i >= 0; --i)
    {
      CharacterPosition savedCharacterPosition = _savedPositionStack.get(i);
      if (savedCharacterPosition.line == lastLine && savedCharacterPosition.column == lastColumn)
      {
        savedCharacterPosition.line = _line;
        savedCharacterPosition.column = _column;
      } else
      {
        break;
      }
    }
  }

  /**
   * Row and column numbers of a character in Writer output
   */
  public static class CharacterPosition
  {

    private int line;
    private int column;

    CharacterPosition(int line, int column)
    {
      this.line = line;
      this.column = column;
    }

    /**
     * 1-based index of line in writer output
     */
    public int getLine()
    {
      return line;
    }

    /**
     * 1-based index of column in writer output
     */
    public int getColumn()
    {
      return column;
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
      {
        return true;
      }
      if (o == null || getClass() != o.getClass())
      {
        return false;
      }
      CharacterPosition characterPosition = (CharacterPosition) o;
      return line == characterPosition.line && column == characterPosition.column;
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(line, column);
    }

    @Override
    public String toString()
    {
      return "CharacterPosition{" + "line=" + line + ", column=" + column + '}';
    }
  }
}
