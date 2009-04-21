package org.openflow.util.string;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Defines some methods for matching and replacement on special conditions for Strings.
 *
 * @author David Underhill
 */
public abstract class StringOps {
    public static String formatBits( long num_bits, boolean toBytes  ) {
        long bytes = num_bits / (toBytes ? 8 : 1);
        int units = 0;
        while( bytes >= 10000 ) {
            bytes /= 1000;
            units += 1;
        }
        String strUnit;
        switch( units ) {
            case  0: strUnit = "";  break;
            case  1: strUnit = "k"; break;
            case  2: strUnit = "M"; break;
            case  3: strUnit = "G"; break;
            case  4: strUnit = "T"; break;
            case  5: strUnit = "P"; break;
            default: strUnit = "?"; break;
        }
        
        return Long.toString( bytes ) + strUnit + (toBytes ? "B" : "b");
    }
    
    public static String formatBitsPerSec(long num_bits, boolean toBytes) {
        return formatBits(num_bits, toBytes) + "ps";
    }
    
    public static String formatBitsPerSec(long num_bits) {
        return formatBitsPerSec(num_bits, false);
    }
    
    public static String formatBitsPerSec(float util, long max_rate) {
        return formatBitsPerSec((int)(max_rate * util), false);
    }
    
    public static String formatSecs(int num_msecs) {
        int hours = num_msecs / 3600000; num_msecs -= 3600000 * hours;
        int mins = num_msecs / 60000; num_msecs -= 60000 * mins;
        int secs = num_msecs / 1000; num_msecs -= 1000 * secs;
        
        if(hours > 0)
            return hours + "h:" + mins + "m:" + secs + "." + num_msecs + "sec";
        if(mins > 0)
            return mins + "m:" + secs + "." + num_msecs + "sec";
        if(secs > 0)
            return secs + "." + num_msecs + "sec";
        return num_msecs + "msec";
    }
    
  /**
   * Checks to see if name contains anything other than alphanumeric characters, underscores,
   * or spaces.
   *
   * @param name  the string to check
   *
   * @throws IllegalArgValException  occurs if the name is invalid.
   *
   * @return  name if it is valid.
   */
  public static String checkName(String name) throws IllegalArgValException {
    for (int i = 0; i < name.length(); i++) {
      if (!Character.isLetterOrDigit(name.charAt(i)) && name.charAt(i) != ' ' && name.charAt(i) != '_') {
        throw new IllegalArgValException("The character " + name.charAt(i) + " is not allowed in names.");
      }
    }
    return name;
  }

  /**
   * Make sure the last character in the specified String is a slash unless s
   * is of length 0 in which case the empty string passed in is returned.
   *
   * @param s  the string to check
   *
   * @return  s concatenated with a `/` if it didn't already end with a slash (`\` or `/`)
   */
  public static String endWithSlash(String s) {
    if (s.length() == 0) {
      return s;
    }
    char lastChar = s.charAt(s.length() - 1);
    if (lastChar != '\\' || lastChar != '/') {
      s = s.concat("/");
    }
    return s;
  }

  /**
   * determine the path in the string (all text before and included the last slash mark,
   * or the empty string if there are no slashes)
   *
   * @param fn  the filename to find the path in
   *
   * @return  the path to the file specified in fn included the trailing slash as appropriate
   */
  public static String getPath(String fn) {
    if (fn == null || fn.length() == 0) {
      return "";
    }
    int fslashIndex = fn.lastIndexOf('/');
    int bslashIndex = fn.lastIndexOf('\\');

    int lastSlashIndex = Math.max(fslashIndex, bslashIndex);
    if (lastSlashIndex == -1) {
      return ""; //no slashes
    }
    return fn.substring(0, lastSlashIndex + 1);
  }

  /**
   * Gets the contents of a file as a String
   *
   * @param fn  the file to read
   *
   * @return  the contents of the file as a string
   *
   * @throws java.io.IOException  occurs if the file is invalid or unable to be properly read
   */
  public static String getFileAsString(String fn) throws java.io.IOException {
    java.io.FileInputStream in = new java.io.FileInputStream(fn);
    byte[] b = new byte[in.available()];
    in.read(b);
    in.close();

    return new String(b);
  }

  /**
   * Gets the text following the last slash (\ or /) in a string (the "file title")
   *
   * @param s  the string to work with
   *
   * @return  all text is s following the last slash, or s if there were no slashes, or s if s is length 1.
   */
  public static String getFileTitle(String s) {
    int index1 = s.lastIndexOf('\\');
    int index2 = s.lastIndexOf('/');
    if (index1 <= 0 && index2 <= 0) {
      return s;
    }
    if (index1 > index2) {
      return s.substring(index1 + 1);
    } else {
      return s.substring(index2 + 1);
    }
  }

  /**
   * Removes a specified number of extensions from s
   *
   * @param s  the string to remove extensions from (.ext)
   * @param numExtToStrip  the number of extensions to remove
   *
   * @return  s without the specified number of extensions (if s has fewer extensions that numExtToStrip
   *          then all extensions are stripped from s).  The return may be of zero length.
   */
  public static String stripExtensions(String s, int numExtToStrip) {
    int index = s.lastIndexOf('.');
    while (numExtToStrip-- > 0) {
      if (index >= 0) {
        s = s.substring(0, index);
      } else {
        break;
      }
      index = s.lastIndexOf('.');
    }

    return s;
  }

  /**
   * replaces all occurs of searchTerm in s which don't 
   * have a letter, digit, or underscore on either side
   *
   * @param s            the string being searched
   * @param searchTerm   what is being searched for
   * @param replaceTerm  what searchTerm is being replaced with
   *
   * @return  s with all occurrences of searchTerm replaced with replaceTerm
   */
  public static String replace(String s, String searchTerm, String replaceTerm) {
    String ret = s;
    int indexGood = indexOf(ret, searchTerm);
    int index = ret.indexOf(searchTerm);
    while (index != -1) {
      int nextStartIndex = index;
      if (indexGood != -1) {
        ret = ret.substring(0, index) + replaceTerm + ret.substring(index + searchTerm.length());
        nextStartIndex += replaceTerm.length();
      } else {
        nextStartIndex += searchTerm.length();
      }
      index = ret.indexOf(searchTerm, nextStartIndex);
      indexGood = indexOf(ret, searchTerm, nextStartIndex);
    }
    return ret;
  }

  /**
   * Replaces the text between two strings (and removes the specified strings themselves too)
   *
   * @param str   the string to search
   * @param s1    the first string to find
   * @param s2    the second string to find
   * @param repl  the string to use to replace the range from the beginning s1 to the end s2 inclusive
   *
   * @return  the resulting string, or the string str which was passed in if s1 or s2 are not found
   */
  public static String replaceBetweenStrings(String str, String s1, String s2, String repl) {
    int index1 = str.indexOf(s1);
    if (index1 == -1) {
      return str;
    }
    int index2 = str.indexOf(s2, index1 + s1.length());
    if (index2 == -1) {
      return str;
    }
    String front = str.substring(0, index1);
    String end = str.substring(index2 + s2.length());

    return front + repl + end;
  }

  /**
   * Selects the text between two strings
   *
   * @param str  the string to search
   * @param s1   the first string to find
   * @param s2   the second string to find
   *
   * @return the text between s1 and s2 (trimmed) or an empty string if either string isn't found
   */
  public static String selectBetweenStrings(String str, String s1, String s2) {
    int index1 = str.indexOf(s1);
    if (index1 == -1) {
      return "";
    }
    int index2 = str.indexOf(s2, index1 + s1.length());
    if (index2 == -1) {
      return "";
    }
    return str.substring(index1 + s1.length(), index2);
  }

  /**
   * Writes a string to a file
   *
   * @param fn   the file to write the string to
   * @param str  the string to write
   *
   * @throws java.io.IOException  occurs if the file is invalid or unable to be properly read
   */
  public static void writeStringToFile(String fn, String str) throws java.io.IOException {
    java.io.PrintStream ps = new java.io.PrintStream(new java.io.FileOutputStream(fn));
    ps.println(str);
    ps.close();
  }

  /**
   * finds the searchTerm in String s from startIndex which doesn't 
   * have a letter, digit, or underscore on either side of it
   *
   * @param s           the string being searched
   * @param searchTerm  what is being searched for
   *
   * @return  the index in s at which searchTerm occurs or -1 if it is not found
   */
  public static int indexOf(String s, String searchTerm) {
    return indexOf(s, searchTerm, 0);
  }

  /**
   * finds the searchTerm in String s from startIndex which doesn't 
   * have a letter, digit, or underscore on either side of it
   *
   * @param s           the string being searched
   * @param searchTerm  what is being searched for
   * @param startIndex  where to start looking
   *
   * @return  the index in s at which searchTerm occurs or -1 if it is not found
   */
  public static int indexOf(String s, String searchTerm, int startIndex) {
    int index = s.indexOf(searchTerm, startIndex);
    if (index == -1) {
      return -1;
    }
    char charBefore = (index > 0) ? s.charAt(index - 1) : ' ';
    char charAfter = (index + searchTerm.length() < s.length()) ? s.charAt(index + searchTerm.length()) : ' ';

    //return -1 if not found outside an identifier
    if (Character.isLetterOrDigit(charBefore) || charBefore == '_' || Character.isLetterOrDigit(charAfter) || charAfter == '_') {
      return -1;
    } else {
      return index;
    }
  }

  /**
   * inserts a newline after every 80 characters
   *
   * @param s           the string to split
   *
   * @return the split up string
   */
  public static String splitIntoLines(String s) {
    return splitIntoLines(s, 80);
  }

  /**
   * inserts a newline after every maxLineLen characters
   *
   * @param s           the string to split
   * @param maxLineLen  the max length (in characters) a line may be
   *
   * @return the split up string
   */
  public static String splitIntoLines(String s, int maxLineLen) {
    return splitIntoLines(s, maxLineLen, "\n", true); //add whatever remains of s
  }

  /**
   * inserts a newline string specified by the user after every maxLineLen characters
   *
   * @param s                  the string to split
   * @param maxLineLen         the max length (in characters) a line may be
   * @param newlineIdentifier  the string to use to split lines up
   * @param trim               whether or not trim whitespace of each line's ends
   *
   * @return the split up string
   */
  public static String splitIntoLines(String s, int maxLineLen, String newlineIdentifier, boolean trim) {
    return splitIntoLines(s, maxLineLen, maxLineLen, newlineIdentifier, trim);
  }

  /**
   * inserts a newline string specified by the user after every maxLineLen characters
   *
   * @param s                  the string to split
   * @param firstLineMaxLen    the max length (in characters) the first line may be
   * @param maxLineLen         the max length (in characters) a line may be
   * @param newlineIdentifier  the string to use to split lines up
   * @param trim               whether or not trim whitespace of each line's ends
   *
   * @return the split up string
   */
  public static String splitIntoLines(String s, int firstLineMaxLen, int maxLineLen, String newlineIdentifier, boolean trim) {
    String ret = "";

    if (trim) {
      s = s.trim();
    }
    
    int len = firstLineMaxLen;
    while (s.length() > len) {
      //find a good place to split (don't split a word!)
      int actualSplit = -1;

      //look for whitespace to break on (but don't go to the first char!)
      char c = '\0';
      for (int i = len - 1; i > 0; i--) {
        c = s.charAt(i);
        if(c==' ' || c=='\t' || c=='-') {
          actualSplit = i;
          break;
        }
      }

      // choose the break spot
      int end, start;
      if(actualSplit < 0) {
          end = len;
          start = end;
      } 
      else {
          end = actualSplit;
          start = actualSplit + (c=='-' ? 0 : 1);
      }
      
      //add the new line
      ret = ret.concat(s.substring(0, end) + newlineIdentifier);

      //remove the characters just added to ret
      s = s.substring(start);

      if (trim) {
        s = s.trim();
      }
      len = maxLineLen;
    }

    return ret.concat(s); //add whatever remains of s
  }
  
  /**
   * Breaks the args string into an array of strings where each element is an
   * individual argument.  Arguments are separated by spaces, though spaces can 
   * be included by enclosing the argument in quote (") marks.  A beginning quote 
   * mark must be preceded by a space.  An ending quote mark must be followed by 
   * a space.
   * 
   * @param args  the arguments string to parse
   * 
   * @return an array of individual arguments, or null if args was null or the empty string, or no args were parsed
   */
  public static String[] splitArgs( String args ) {
    if( args==null || args.length()==0 ) return null;
    
    //split by spaces
    String[] split = args.split( " " );
    
    //process each space-separated string
    String quote = null;
    LinkedList<String> actualArgs = new LinkedList<String>();
    for( String s : split ) {
      if( s.length()>0 ) {
        if( quote == null ) {
          //a quote is starting
          if (s.charAt(0) == '\"') {
            //only quotes => empty string
            if( s.equals("\"\"") )
              actualArgs.add("");
            
            //see if a quotes used around a single string without spaces
            else if( s.length()>1 && s.charAt(s.length()-1)=='"' )
              actualArgs.add( s.substring(1, s.length()-1) );
            
            //just starting the quote (was a space in b/w the quotes)
            else
              quote = s.substring(1);
          
          //just a regular argument
          } else
            actualArgs.add( s );
        
        //else we're in a quote
        }
        else {
          //see if there is an end quote here
          int quoteIndex = s.indexOf('"');
          
          //if so, add a the terminating portion of the string
          if( quoteIndex >= 0 ) {
            if( quoteIndex == 0 )
              actualArgs.add(quote.concat(" "));
            else
              actualArgs.add(quote.concat(" " + s.substring(0, s.length() - 1)));
              
            //no longer in a quote
            quote = null;
          }
          
          //still in the quote, just add this next piece to the overall text
          else
            quote = quote.concat(" " + s);
        }
      }
    }
    
    //if no args were found, return null
    if( actualArgs.size() == 0 )
      return null;
    
    Iterator<String> itr = actualArgs.iterator();
    String[] ret = new String[actualArgs.size()];
    for( int i=0; itr.hasNext(); i++ )
      ret[i] = itr.next();
    
    return ret;
  }
}
