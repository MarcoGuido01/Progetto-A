package partitaOffline.model;

import dominio.eccezioni.GameOverException;
import dominio.giocatori.GiocatoreUmano;
import dominio.events.*;
import dominio.elementi_di_gioco.Mazzo;
import dominio.giocatori.BotFacile;
import dominio.giocatori.Giocatore;
import dominio.classi_dati.DifficoltaBot;
import dominio.classi_dati.StatoMano;
import dominio.classi_dati.TipoPagamento;
import dominio.eccezioni.CanzoneNonTrovataException;
import dominio.eccezioni.CaricamentoCanzoneException;
import dominio.eccezioni.FineMazzoException;
import dominio.eccezioni.MazzierePerdeException;
import dominio.eccezioni.SballatoException;
import dominio.eccezioni.SetteeMezzoException;
import dominio.eccezioni.SetteeMezzoRealeException;
import dominio.elementi_di_gioco.Carta;
import dominio.giocatori.BotDifficile;
import dominio.giocatori.BotMedio;
import dominio.elementi_di_gioco.Regole;
import java.util.ArrayList;
import java.util.Observable;
import dominio.musica.AudioPlayer;
import partitaOffline.events.GiocatoreLocaleEventListener;
import dominio.eccezioni.DatoGiaPresenteException;
import dominio.pagamenti.Pagamento;
import dominio.pagamenti.PagamentoFactory;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PartitaOfflineModel extends Observable {
    private Regole regole_di_gioco = new Regole();
    private AudioPlayer audio = new AudioPlayer();
    private ArrayList<Giocatore> giocatori=new ArrayList<>();
    private GiocatoreUmano giocatore_locale;
    private final Mazzo mazzo = new Mazzo();
    private Giocatore mazziere = null;
    private Giocatore next_mazziere = null;
    private int n_bot;
    private int n_bot_sconfitti = 0;
    private DifficoltaBot difficolta_bot;
    private int fiches_iniziali;
    private String nome_giocatore;

    
    /**
     *
     * @param numero_bot numero di bot iniziali
     * @param fiches_iniziali numero di fiches iniziali per ogni giocatore
     * @param difficolta_bot difficoltá di tutti i bot della partita
     */
    public PartitaOfflineModel(int numero_bot, DifficoltaBot difficolta_bot, int fiches_iniziali){
        this.n_bot = numero_bot;
        this.difficolta_bot = difficolta_bot;
        this.fiches_iniziali = fiches_iniziali;
        
        try {
            inizializza_audio();
            audio.riproduciInLoop("soundTrack");
        } catch (CaricamentoCanzoneException ex) {
            this.setChanged();
            this.notifyObservers(new Error("Errore: Impossibile caricare la canzone " + ex.getCanzone()));
        } catch (CanzoneNonTrovataException ex) {
            this.setChanged();
            this.notifyObservers(new Error("Errore: " +ex.getCanzone() + " non caricata/o"));
        }
    }
    
    /**
     * Consente di giocare una partita di sette e mezzo.
     * @throws InterruptedException
     */
    public void gioca(){
        estrai_mazziere();
        
        this.setChanged();
        this.notifyObservers(new EstrattoMazziere());
        
        mazzo.aggiornaFineRound();
        mazzo.rimescola();
        
        this.setChanged();
        this.notifyObservers(new MazzoRimescolato());
        
        while(true){
            try {
                gioca_round();
                calcola_risultato();
            } catch (MazzierePerdeException ex) {
                this.setChanged();
                this.notifyObservers(new MazzierePerde());
                mazziere.azzeraFiches();
                mazziere_successivo();
                
            }
            try {
                fine_round();
            } catch (GameOverException ex) {
                break;
            }
            mazzo.aggiornaFineRound();
            if(n_bot_sconfitti == n_bot){
                this.setChanged();
                this.notifyObservers(new Vittoria());
                break;
            }
        }
    }
    
    private void inizializza_audio() throws CaricamentoCanzoneException{
        audio.carica("LoungeBeat.wav", "soundTrack");
        audio.carica("deckShuffle.wav", "deckShuffle");
        audio.carica("ApplausiSetteEMezzo.wav", "applausiSEM");
    }
    
    /**
     * inizializza la partita settando il numero di bot, le fiches iniziali e la difficoltà dei bot
     */
    public void inizializza_partita(){
        inizializza_bots(n_bot, fiches_iniziali, difficolta_bot); 
        inizializza_giocatore(fiches_iniziali);
    }
    
    private void inizializza_bots(int numero_bot, int fiches_iniziali, DifficoltaBot difficolta_bot){
        for(int i = 0; i < numero_bot; i++){
            switch(difficolta_bot){
                case Facile : {
                    giocatori.add(new BotFacile("bot"+i, fiches_iniziali, mazzo)); //nomi bot: bot0, bot1, ...
                    break;
                }
                case Medio : {
                    giocatori.add(new BotMedio("bot"+i, fiches_iniziali, mazzo));
                    break;
                }
                case Difficile : {
                    giocatori.add(new BotDifficile("bot"+i, fiches_iniziali, mazzo));
                    break;
                }       
            }
        }
    }
    
    private void inizializza_giocatore(int fiches_iniziali){
        this.setChanged();
        this.notifyObservers(new RichiediNome());
        giocatore_locale = new GiocatoreUmano(nome_giocatore,fiches_iniziali);
        giocatori.add(giocatore_locale);
        
        
    }
    
    /**
     * 
     * @param nome_giocatore nome del giocatore
     */
    public void setNomeGiocatore(String nome_giocatore){
        this.nome_giocatore = nome_giocatore;
    }

    private void estrai_mazziere(){
        Carta carta_estratta;
        
        mazzo.mischia();
        
        for(Giocatore giocatore : giocatori){
            while(true){
                try {
                    carta_estratta = mazzo.estraiCarta();
                    giocatore.prendiCartaIniziale(carta_estratta);
                    break;
                }catch (FineMazzoException ex) {
                    mazzo.rimescola(); //non dovrebbe accadere
                }
            }
            mazziere = regole_di_gioco.cartaPiuAlta(mazziere, giocatore);
        }
        mazziere.setMazziere(true);
    }

    private void gioca_round() throws MazzierePerdeException{
        int pos_mazziere = giocatori.indexOf(mazziere);
        int pos_next_giocatore = pos_mazziere + 1;
        Giocatore giocatore;
        
        inizializza_round();
        distribuisci_carta_coperta();
        effettua_puntate();
        for(int i = 0; i < giocatori.size(); i++){
            if(pos_next_giocatore == giocatori.size()){
                pos_next_giocatore = 0;
            }
            giocatore = getProssimoGiocatore(pos_next_giocatore);
            if(! giocatore.haPerso()){  
                esegui_mano(giocatore);
                if(giocatore instanceof GiocatoreUmano && giocatore.getStatoMano() != StatoMano.OK){
                    
                    this.setChanged();
                    this.notifyObservers(new RisultatoManoParticolare());
                }
            }
            if(! (giocatore instanceof GiocatoreUmano)){
                this.setChanged();
                this.notifyObservers(new FineManoAvversario(giocatore.getNome(), giocatore.getCarteScoperte(),giocatore.getStatoMano(), giocatore.getPuntata()));
            }
            pos_next_giocatore += 1;
        }
    }
    
    private void inizializza_round(){
        for(Giocatore giocatore : giocatori){
            giocatore.inizializzaMano();
        }
        next_mazziere = null;
    }
    
    private void distribuisci_carta_coperta(){
        Carta carta_estratta;
        
        for(Giocatore giocatore : giocatori){
            while(true){
                try {
                    if(! giocatore.haPerso()){
                        carta_estratta = mazzo.estraiCarta();
                        giocatore.prendiCartaIniziale(carta_estratta);
                    }
                    break;
                } catch (FineMazzoException ex) {
                    mazzo.rimescola();
                    
                    this.setChanged();
                    this.notifyObservers(new MazzoRimescolato());
                    
                    this.mazziere_successivo();
                }
            }
        }
    }
    
    private void effettua_puntate() {
        for(Giocatore giocatore : giocatori){
            if(! giocatore.equals(mazziere)){
                giocatore.effettuaPuntata();
            }
        }
        if(mazziere instanceof GiocatoreUmano){
                GiocatoreUmano giocatore = (GiocatoreUmano) mazziere;
//                giocatore.stampaCartaCoperta();
            }
    }
    
    private Giocatore getProssimoGiocatore(int posizione){
        return giocatori.get(posizione);
    }
    
    private void esegui_mano(Giocatore giocatore) throws MazzierePerdeException{
        Carta carta_estratta = null;
        boolean continua = true;
        Pagamento pagamento = PagamentoFactory.getPagamento(TipoPagamento.Reale);
        
        while(continua){
            continua = giocatore.effettuaGiocata();
            if(continua){
                try {
                    carta_estratta = mazzo.estraiCarta();
                } catch (FineMazzoException ex) {
                    mazzo.rimescola();
                    
                    this.setChanged();
                    this.notifyObservers(new MazzoRimescolato());
                    
                    mazziere_successivo();
                    try {
                        carta_estratta = mazzo.estraiCarta();
                    } catch (FineMazzoException ex1) {
                        //////////////////////////////
                    }
                }
                try {
                    giocatore.chiediCarta(carta_estratta);
                } catch (SballatoException ex) {
                    giocatore.setStatoMano(StatoMano.Sballato);
                    if(!giocatore.isMazziere()){
                        pagamento.normale(giocatore, mazziere, 1); //giocatore se sballa paga subito.
                    }
                    continua = false;
                } catch (SetteeMezzoRealeException ex) {
                    giocatore.setStatoMano(StatoMano.SetteeMezzoReale);
                    continua = false;
                } catch (SetteeMezzoException ex) {
                    giocatore.setStatoMano(StatoMano.SetteeMezzo);
                    continua = false;
                }
            }
        }
    }
    
    private void calcola_risultato() throws MazzierePerdeException{ 
        Giocatore mazziere_prova = controllaMazziere();
        double fichesMazziereProva = (double) mazziere_prova.getFiches();
        Pagamento pagamento = PagamentoFactory.getPagamento(TipoPagamento.Reale);
        
        if(fichesMazziereProva >0){
            for(Giocatore giocatore : giocatori){
                if(! giocatore.isMazziere()){              
                    next_mazziere = regole_di_gioco.risultatoMano(mazziere, giocatore, next_mazziere, 1, pagamento);
                }
            }
        }
        else {
            double fichesAttualiMazziere= (double) mazziere.getFiches();
            double percentuale=(double)(fichesAttualiMazziere/(fichesAttualiMazziere-fichesMazziereProva));            
            for(Giocatore giocatore : giocatori){
                if(! giocatore.isMazziere()){              
                    next_mazziere = regole_di_gioco.risultatoMano(mazziere, giocatore, next_mazziere, percentuale, pagamento);
                }
            }
             throw new MazzierePerdeException();
        }
           
    }        

    private Giocatore controllaMazziere() {
        Giocatore mazziere_prova = (Giocatore) mazziere.clone(); //clone del mazziere su cui verranno effettuati i calcoli
        Pagamento pagamento = PagamentoFactory.getPagamento(TipoPagamento.Virtuale);
        
        for(Giocatore giocatore : giocatori){
            if(! giocatore.isMazziere()){
                this.regole_di_gioco.risultatoMano(mazziere_prova, giocatore, next_mazziere, 1, pagamento);                      
            }
        }
        return mazziere_prova;
    }
    
   private void mazziere_successivo(){
       int pos_next_mazziere = giocatori.indexOf(mazziere) + 1;
       if(pos_next_mazziere == giocatori.size()){
           pos_next_mazziere = 0;
       }
       for(int i = 0; i < giocatori.size(); i++){
           if(giocatori.get(pos_next_mazziere).haPerso()){
                pos_next_mazziere += 1;
                if(pos_next_mazziere == giocatori.size()){
                    pos_next_mazziere = 0;                
                }              
           } else {
               next_mazziere = giocatori.get(pos_next_mazziere);
               break;
           }
       }
   }
    
    private void fine_round() throws GameOverException{
        ArrayList<Giocatore> giocatori_sconfitti = new ArrayList<>();
        boolean game_over = false;
        for(Giocatore giocatore : giocatori){
            
            this.setChanged();
            this.notifyObservers(new FineRound(giocatore));
            if(giocatore.getFiches() == 0 && ! giocatore.haPerso()){
                if(giocatore instanceof GiocatoreUmano){
                    giocatore.perde();
                    game_over = true;
                } else {
                    giocatore.perde();
                    n_bot_sconfitti += 1;
                    giocatori_sconfitti.add(giocatore);
                }
            }
        }
        giocatori.removeAll(giocatori_sconfitti);
        if(game_over){
            this.setChanged();
            this.notifyObservers(new GameOver());
            game_over();
        }
        if(next_mazziere != null){
            aggiorna_mazziere();
            this.setChanged();
            this.notifyObservers(new AggiornamentoMazziere());
        }
    }
    
    private void aggiorna_mazziere(){
        mazziere.setMazziere(false);
        next_mazziere.setMazziere(true);
        mazziere = next_mazziere;
    }

    private void game_over() throws GameOverException{
        throw new GameOverException();
    }


    /**
     * 
     * @return numero dei bot
     */
    public int getN_bot() {
        return n_bot;
    }

    /**
     * 
     * @return difficoltà dei bot
     */
    public DifficoltaBot getDifficolta_bot() {
        return difficolta_bot;
    }

    /**
     * 
     * @return quantità di fiches iniziali
     */
    public int getFiches_iniziali() {
        return fiches_iniziali;
    }
    
    /**
     * 
     * @param l evento
     */
    public void addGiocatoreLocaleEventListener(GiocatoreLocaleEventListener l){
        giocatore_locale.addGiocatoreLocaleEventListener(l);
    }
    
    /**
     * 
     * @param l evento
     */
    public void removeGiocatoreLocaleEventListener(GiocatoreLocaleEventListener l){
        giocatore_locale.removeGiocatoreLocaleEventListener(l);
    }

    /**
     * 
     * @return lista di giocatori
     */
    public ArrayList<Giocatore> getGiocatori() {
        return giocatori;
    }

    /**
     * 
     * @return mazziere
     */
    public Giocatore getMazziere() {
        return mazziere;
    }

    /**
     * 
     * @return giocatore locale
     */
    public GiocatoreUmano getGiocatoreLocale() {
        return giocatore_locale;
    }
    
    /**
     * 
     * @return audio 
     */
    public AudioPlayer getAudio() {
        return audio;
    }
}