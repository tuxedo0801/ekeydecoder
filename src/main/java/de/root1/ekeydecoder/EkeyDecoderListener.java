/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.ekeydecoder;

/**
 *
 * @author achristian
 */
public interface EkeyDecoderListener {

    public void fingerhashReceived(int fingerhash);
    
}
