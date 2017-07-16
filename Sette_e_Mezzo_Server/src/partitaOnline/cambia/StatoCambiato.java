/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package partitaOnline.cambia;

import dominio.classi_dati.Stato;

/**
 *
 * @author root
 */
public class StatoCambiato {
    private String nome;
    private Stato stato;

    /**
     * 
     * @param nome nome giocatore
     * @param stato stato giocatore
     */
    public StatoCambiato(String nome, Stato stato) {
        this.nome = nome;
        this.stato = stato;
    }

    @Override
    public String toString() {
        return "cambia\tStatoCambiato\t" + nome + " " + stato;
    }
    
    
}
