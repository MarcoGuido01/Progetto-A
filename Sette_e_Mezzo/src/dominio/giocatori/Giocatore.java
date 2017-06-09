package dominio.giocatori;

import dominio.eccezioni.SetteeMezzoException;
import dominio.eccezioni.SetteeMezzoRealeException;
import dominio.eccezioni.SballatoException;
import dominio.classi_dati.Giocata;
import dominio.classi_dati.StatoMano;
import dominio.eccezioni.FineMazzoException;
import dominio.elementi_di_gioco.Carta;
import java.util.ArrayList;

public abstract class Giocatore {

    private final String nome;
    private int fiches;
    private boolean mazziere;
    protected Carta carta_coperta;
    private int puntata;
    protected ArrayList<Carta> carte_scoperte = new ArrayList<>();
    protected ValoreMano valore_mano;
    private StatoMano stato;
    private boolean perso = false;

    /**
     *
     * @param nome nome del giocatore.
     * @param fiches numero di fiches iniziali del giocatore.
     */
    public Giocatore(String nome, int fiches) {
        this.nome = nome;
        this.fiches = fiches;
        this.valore_mano = new ValoreMano();
    }

    /**
     * Inizializzazione prima di giocare una nuova mano. - Elimina la carta
     * coperta della mano precedente - Elimina le carte scoperte della mano
     * precedente - Inizializza la puntata a 0 - Inizializza il valore_mano a 0
     * - Inizializza lo stato della mano a OK
     *
     */
    public void inizializza_mano() {
        carta_coperta = null;
        puntata = 0;
        carte_scoperte.clear();
        valore_mano.inizializza();
        stato = StatoMano.OK;
    }

    /**
     * Prende la prima carta della mano e la usa come carta_coperta.
     *
     * @param carta carta pescata
     * @throws FineMazzoException avvisa se il mazzo non ha piú carte estraibili
     */
    public void prendi_carta_iniziale(Carta carta) throws FineMazzoException {
        carta_coperta = carta;
        valore_mano.aggiorna(carta_coperta, carte_scoperte);
    }

    /**
     * Effettua una giocata.
     *
     * @return continua o meno la mano.
     */
    public boolean effettua_giocata() {
        Giocata giocata = decidi_giocata();
        return gioca(giocata);
    }

    private boolean gioca(Giocata giocata) {
        switch (giocata) {
            case Carta:
                return true;
            case Sto:
                return false;
            default:
                return true; //impossibile ma senza da errore
        }
    }

    /**
     * Consente di decidere la giocata da effettuare.
     *
     * @return la giocata scelta
     */
    protected abstract Giocata decidi_giocata();

    /**
     * Effettua una puntata.
     *
     */
    public void effettua_puntata() {
        int valore_puntata = decidi_puntata();
        punta(valore_puntata);
    }

    /**
     * Consente di decidere la puntata da effettuare.
     *
     * @return il valore della puntata scelta
     */
    protected abstract int decidi_puntata();

    public void punta(int puntata) {
        fiches = fiches - puntata;
        this.puntata = puntata;
    }
    
    public int aggiungi_puntata_reale() {
        fiches = fiches - (2 * puntata);
        if (fiches < 0) {
            int buf = fiches;
            fiches = 0;
            return puntata + (buf + puntata);
        }
        return puntata * 2;
    }

    /**
     * Prende la carta passata e la usa come carta scoperta. Aggiorna e
     * controlla il valore della mano.
     *
     * @param carta Carta pescata
     * @throws SballatoException
     * @throws SetteeMezzoRealeException
     * @throws SetteeMezzoException
     */
    public void chiedi_carta(Carta carta) throws SballatoException, SetteeMezzoRealeException, SetteeMezzoException {
        carte_scoperte.add(carta);
        valore_mano.aggiorna(carta_coperta, carte_scoperte);
        valore_mano.controlla(carta_coperta, carte_scoperte);
    }

    /**
     * Incassa una vincita aggiungendola alle proprie fiches.
     *
     * @param vincita numero di fiches vinte
     */
    public void riscuoti(int vincita) {
        fiches = fiches + puntata + vincita;
    }

    /**
     * Azzera il numero di fiches del giocatore.
     */
    public void azzera_fiches() {
        fiches = 0;
    }

    /**
     * Imposta il booleano perso a true.
     */
    public void perde() {
        perso = true;
    }

    public boolean haPerso() {
        return perso;
    }

    public boolean isMazziere() {
        return mazziere;
    }

    public void setMazziere(boolean mazziere) {
        this.mazziere = mazziere;
    }

    public void setStatoMano(StatoMano stato) {
        this.stato = stato;
    }

    public String getNome() {
        return nome;
    }

    public int getFiches() {
        return fiches;
    }

    public int getPuntata() {
        return puntata;
    }

    public double getValoreMano() {
        return valore_mano.getValore();
    }

    public StatoMano getStatoMano() {
        return stato;
    }

    public Carta getCartaCoperta() {
        return carta_coperta;
    }

    public ArrayList<Carta> getCarteScoperte() {
        return carte_scoperte;
    }

    public ArrayList<Carta> getTutteLeCarte() {
        ArrayList<Carta> carte = new ArrayList<>();
        carte.add(carta_coperta);
        carte.addAll(carte_scoperte);
        return carte;
    }

    public Carta getUltimaCartaOttenuta() {
        return carte_scoperte.get(carte_scoperte.size() - 1);
    }
}
