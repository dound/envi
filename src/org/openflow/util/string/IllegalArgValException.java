package org.openflow.util.string;

/**
 * A checked Exception replacement for IllegalArgumentException
 *
 * @author David Underhill
 */
public class IllegalArgValException extends Exception {
    
    /** default constructor */
    public IllegalArgValException() {
        super( "Illegal Argument Value Exception" );
    }
    
    /** specify the message for the Exception */
    public IllegalArgValException( String msg ) {
        super( msg );
    }
}
