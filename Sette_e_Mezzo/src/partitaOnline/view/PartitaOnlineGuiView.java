package partitaOnline.view;

import dominio.classi_dati.StatoMano;
import dominio.eccezioni.CanzoneNonTrovataException;
import dominio.eccezioni.CaricamentoCanzoneException;
import dominio.eccezioni.MattaException;
import dominio.elementi_di_gioco.Carta;
import dominio.giocatori.GiocatoreOnline;
import dominio.gui.Sfondo;
import dominio.musica.AudioPlayer;
import dominio.view.ViewEventListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import menuPrincipale.MenuPrincipaleGui;
import partitaOnline.events.*;
import partitaOnline.controller.PartitaOnlineController;
import dominio.events.*;

public class PartitaOnlineGuiView extends JFrame implements Observer {

    private CopyOnWriteArrayList<ViewEventListener> listeners;
    private PartitaOnlineController controller;
    private Sfondo sfondo;
    private String puntataStr, giocataStr;
    private JTextField puntata;
    private JButton carta, stai;
    private JLabel msgDaStampare;
    private boolean needCartaCoperta = true, mazziereEstratto = false, needToMarkMazziere = false,
            needStatoCambiato = false, partitaIniziata = false, esciAFineRound = false;
    private ArrayList<JLabel> carteCoperteAvversari = new ArrayList<>();
    private Map<String, JLabel> valoriMano = new HashMap<>();
    private final int pausa_breve = 1000; //ms
    private final int pausa_lunga = 2000; //ms
    private AudioPlayer audio = new AudioPlayer();
    private JLabel imgSalaAttesa, fraseSalaAttesa;
    private JButton esci, esciDaSolo;

    /**
     * 
     * @param controller controller della partita online
     */
    public PartitaOnlineGuiView(PartitaOnlineController controller) {
        listeners = new CopyOnWriteArrayList<>();
        this.controller = controller;
        this.controller.addObserver(this);
        listeners.add(controller);

        setTitle("Sette e Mezzo");
        setPreferredSize(new Dimension(1280, 720));
        setMinimumSize(new Dimension(1280, 720));
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        sfondo = new Sfondo("dominio/immagini/sfondo.png", 1275, 690);
        sfondo.setBounds(0, 0, 1280, 720);
        add(sfondo);

        try {
            inizializza_audio();
            audio.riproduciInLoop("soundTrack");
        } catch (CanzoneNonTrovataException ex) {
            Logger.getLogger(PartitaOnlineGuiView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CaricamentoCanzoneException ex) {
            Logger.getLogger(PartitaOnlineGuiView.class.getName()).log(Level.SEVERE, null, ex);
        }

        setVisible(true);

        inizializzaCartaStaiButtons();
        inizializzaExitButtons();
        inizializza_salaAttesa();
        if (!partitaIniziata) {
            sfondo.add(imgSalaAttesa);
            sfondo.add(fraseSalaAttesa);
            sfondo.add(esciDaSolo);
            sfondo.repaint();
        }
    }
    
    private void inizializzaCartaStaiButtons() {
        carta = new JButton(caricaImmagine("dominio/immagini/carta.png"));
        stai = new JButton(caricaImmagine("dominio/immagini/stai.png"));

        carta.setBounds(1060, 580, 140, 56);
        stai.setBounds(1060, 500, 140, 56);

        carta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                giocataStr = "carta";
            }
        });
        
        stai.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                giocataStr = "sto";
            }
        });
    }

    private void inizializza_salaAttesa() {
        imgSalaAttesa = new JLabel(caricaImmagine("dominio/immagini/salaAttesa.jpg"));
        fraseSalaAttesa = new JLabel("In attesa di altri giocatori");

        Font font = new Font("Sala Attesa", Font.BOLD, 40);
        fraseSalaAttesa.setFont(font);
        fraseSalaAttesa.setForeground(Color.black);
        int strWidth = fraseSalaAttesa.getFontMetrics(font).stringWidth("In attesa di altri giocatori");

        imgSalaAttesa.setBounds(this.getWidth() / 2 - 350, 100, 700, 557);
        fraseSalaAttesa.setBounds(this.getWidth() / 2 - strWidth / 2, 10, strWidth, 100);
    }

    private void inizializzaExitButtons() {
        esci = new JButton(caricaImmagine("dominio/immagini/esci.png"));
        esci.setBounds(35, 600, 96, 58);
        esci.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                esciAFineRound = true;
                controller.riceviEventoDaVista(new Esce());
                
                Font font = new Font("ExitMsg", Font.BOLD, 25);
                JLabel msg = new JLabel("Uscita a fine round");
                msg.setFont(font);
                msg.setForeground(Color.red);
                int strWidth = msg.getFontMetrics(font).stringWidth("Uscita a fine round");
                msg.setBounds(35, 610, strWidth, 60);

                sfondo.remove(esci);
                sfondo.add(msg);
                sfondo.repaint();
            }
        });
        
        esciDaSolo = new JButton(caricaImmagine("dominio/immagini/esci.png"));
        esciDaSolo.setBounds(35, 600, 96, 58);
        esciDaSolo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    audio.ferma("soundTrack");
                    audio.riavvolgi("soundTrack");
                } catch (CanzoneNonTrovataException ex) {
                    Logger.getLogger(PartitaOnlineGuiView.class.getName()).log(Level.SEVERE, null, ex);
                }            
                
                new MenuPrincipaleGui();
                dispose();               
                
                Runnable runnable = new Runnable(){
                    @Override
                    public void run() {
                        controller.esci();
                    }
                };        
                Thread thread = new Thread(runnable);
                thread.start();
            }
        });
    }

    private void inizializza_audio() throws CaricamentoCanzoneException {
        audio.carica("LoungeBeat.wav", "soundTrack");
        audio.carica("deckShuffle.wav", "deckShuffle");
        audio.carica("ApplausiSetteEMezzo.wav", "applausiSEM");
    }

    private void resettaPartita() {
        sfondo.removeAll();
        needCartaCoperta = true;
        mazziereEstratto = false;
        needToMarkMazziere = false;
        needStatoCambiato = false;
        partitaIniziata = false;
        esciAFineRound = false;
        carteCoperteAvversari.clear();
        valoriMano.clear();
        sfondo.repaint();
        controllaUscita();
    }
    
    private void partitaPienaExit() {
        stampaMsg("Tavolo pieno, riprova tra poco!", 60);
        pausa(pausa_lunga);
        try {
            audio.ferma("soundTrack");
            audio.riavvolgi("soundTrack");
        } catch (CanzoneNonTrovataException ex) {
            Logger.getLogger(PartitaOnlineGuiView.class.getName()).log(Level.SEVERE, null, ex);
        }
        controller.esci();
        new MenuPrincipaleGui();
        dispose();
    }

    private ImageIcon caricaImmagine(String nome) {
        ClassLoader caricatore = getClass().getClassLoader();
        URL percorso = caricatore.getResource(nome);
        return new ImageIcon(percorso);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof Error) {
            String errore = ((Error) arg).getMessage();
            JOptionPane.showMessageDialog(null, errore, "Errore", JOptionPane.ERROR_MESSAGE);
        } else if (arg instanceof EstrattoMazziere) {
            estrazioneMazziere();
        } else if (arg instanceof MazzoRimescolato) {
            rimescoloMazzo();
        } else if (arg instanceof DistribuiteCarteCoperte) {
            if (needCartaCoperta) {
                stampaCartaCoperta();
            }
        } else if (arg instanceof FineRound) {
            GiocatoreOnline giocatore = (GiocatoreOnline)((FineRound) arg).getGiocatore();
            if (giocatore != controller.getGiocatoreLocale()) {
                scopriCartaCoperta(giocatore);
            }
            needCartaCoperta = true;
            needStatoCambiato = false;
            stampaValoreManoFineRound(giocatore);
            stampaMessaggioFineRound(giocatore);
            checkFineRound(giocatore);
        } else if (arg instanceof MazzierePerde) {
            //todo mostra che il mazziere ha perso
            stampaMsg(controller.getMazziere().getNome() + " ha perso!", 60);
            pausa(pausa_breve);
            sfondo.remove(msgDaStampare);
            sfondo.repaint();
        } else if (arg instanceof AggiornamentoMazziere) {
            //todo mostra che é stato scelto un nuovo mazziere
            needToMarkMazziere = false;
            sfondo.removeAll();
            estrazioneMazziere();
        } else if (arg instanceof GameOver) {
            stampaMsg("Hai terminato le fiches! Game Over", 50);
            pausa(pausa_lunga);
            try {
                audio.ferma("soundTrack");
                audio.riavvolgi("soundTrack");
            } catch (CanzoneNonTrovataException ex) {
                Logger.getLogger(PartitaOnlineGuiView.class.getName()).log(Level.SEVERE, null, ex);
            }
            controller.esci();
            new MenuPrincipaleGui();
            dispose();
        } else if (arg instanceof RichiediPuntata) {
            stampaPuntataPlayer();
            needStatoCambiato = true;
        } else if (arg instanceof RichiediGiocata) {
            stampaGiocataPlayer();
        } else if (arg instanceof GiocatoreStaPuntando) {
            String nomeGioc = ((GiocatoreStaPuntando) arg).getNome();
            if (getGiocatore(nomeGioc) != controller.getGiocatoreLocale()) {
                stampaMsg(nomeGioc + " sta puntando", 70);
            }
            needStatoCambiato = true;
        } else if (arg instanceof GiocatoreHaPuntato) {
            String nomeGioc = ((GiocatoreHaPuntato) arg).getGiocatore();
            if (getGiocatore(nomeGioc) != controller.getGiocatoreLocale()) {
                sfondo.remove(msgDaStampare);
                sfondo.repaint();
            }
        } else if (arg instanceof GiocatoreIniziaTurno) {
            String nomeGioc = ((GiocatoreIniziaTurno) arg).getGiocatore();
            if (getGiocatore(nomeGioc) != controller.getGiocatoreLocale()) {
                stampaMsg(nomeGioc + " sta giocando", 70);
            }
        } else if (arg instanceof GiocatoreHaPescato) {
            GiocatoreOnline giocatore = ((GiocatoreHaPescato) arg).getGiocatore();
            if (giocatore != controller.getGiocatoreLocale()) {
                stampaGiocatoreHaPescato(giocatore);
            }
        } else if (arg instanceof GiocatoreSta) {
            String nomeGioc = ((GiocatoreSta) arg).getGiocatore();
            if (getGiocatore(nomeGioc) != controller.getGiocatoreLocale()) {
                sfondo.remove(msgDaStampare);
                sfondo.repaint();
            }
        } else if (arg instanceof StatoCambiato) {
            if (needStatoCambiato) {
                String nomeGioc = ((StatoCambiato) arg).getNome();
                if (getGiocatore(nomeGioc) != controller.getGiocatoreLocale()) {
                    stampaStatoAvversario(nomeGioc);
                    sfondo.remove(msgDaStampare);
                    sfondo.repaint();
                } else {
                    stampaStatoPlayer();
                }
            }
        } else if (arg instanceof IniziaPartita) {
            if (imgSalaAttesa != null && fraseSalaAttesa != null) {
                if (esci != null) {
                    sfondo.remove(esci);
                }
                sfondo.remove(imgSalaAttesa);
                sfondo.remove(fraseSalaAttesa);
                sfondo.remove(esciDaSolo);
                sfondo.repaint();
            }
            partitaIniziata = true;
        } else if (arg instanceof ParticellaDiSodio) {
            resettaPartita();
            sfondo.add(imgSalaAttesa);
            sfondo.add(fraseSalaAttesa);
            sfondo.add(esciDaSolo);
            sfondo.repaint();
        } else if (arg instanceof PartitaPiena) {
            pausa(pausa_breve);
            sfondo.removeAll();
            partitaPienaExit();
        }
    }

    // stampa l'animazione di estrazione mazziere, con messaggio finale a mazziere estratto
    private void estrazioneMazziere() {
        int nGiocatori = controller.getGiocatori().size();

        for (int i = 0; i < nGiocatori; i++) {
            stampaNomeFiches(controller.getGiocatori().get(i));
        }

        pausa(pausa_breve);

        int indexAvversario = 0;

        for (int i = 0; i < nGiocatori; i++) {
            stampaValoreMano(controller.getGiocatori().get(i));
            if (controller.getGiocatori().get(i) != controller.getGiocatoreLocale()) {
                stampaCarta((this.getWidth() * (2 * indexAvversario + 1)) / ((nGiocatori - 1) * 2) - 125, 180, controller.getGiocatori().get(i).getCartaCoperta().toString());
                indexAvversario++;
            } else {
                stampaCarta(this.getWidth() / 2 - 125, 3 * this.getHeight() / 4 - 60, controller.getGiocatoreLocale().getCartaCoperta().toString());
            }

            pausa(pausa_breve);
        }

        pausa(pausa_breve);

        GiocatoreOnline mazziere = controller.getMazziere();
        Font font = new Font("Player", Font.BOLD, 70);
        JLabel messaggioMazziere = new JLabel("Il mazziere è: " + mazziere.getNome());
        messaggioMazziere.setFont(font);
        messaggioMazziere.setForeground(Color.black);
        int strWidth = messaggioMazziere.getFontMetrics(font).stringWidth("Il mazziere è: " + mazziere.getNome() + " !!");
        messaggioMazziere.setBounds(this.getWidth() / 2 - strWidth / 2, this.getHeight() / 2 - 60, strWidth, 90);

        sfondo.add(messaggioMazziere);
        sfondo.repaint();

        pausa(pausa_lunga);

        sfondo.removeAll();
        needToMarkMazziere = true;
        mazziereEstratto = true;
        for (int i = 0; i < nGiocatori; i++) {
            stampaNomeFiches(controller.getGiocatori().get(i));
        }

        sfondo.repaint();
    }

    // stampa nome e fiches del giocatore passato, i giocatori hanno nome nero, il mazziere arancione
    private void stampaNomeFiches(GiocatoreOnline giocatore) {
        int numAvversari = controller.getGiocatori().size() - 1;
        int index = controller.getGiocatori().indexOf(giocatore);
        if (index > controller.getGiocatori().indexOf(controller.getGiocatoreLocale())) {
            index--;
        }

        JLabel nomeGiocatore = new JLabel("Nome:   " + giocatore.getNome());
        JLabel fichesGiocatore = new JLabel("Fiches:   " + giocatore.getFiches());

        Font font = new Font("Player", Font.BOLD, 25);
        nomeGiocatore.setFont(font);
        fichesGiocatore.setFont(font);
        fichesGiocatore.setForeground(Color.black);

        if (needToMarkMazziere) {
            if (giocatore.isMazziere()) {
                nomeGiocatore.setForeground(Color.orange);
            } else {
                nomeGiocatore.setForeground(Color.black);
            }
        } else {
            nomeGiocatore.setForeground(Color.black);
        }

        if (giocatore != controller.getGiocatoreLocale()) { // avversari
            nomeGiocatore.setBounds((this.getWidth() * (2 * index + 1)) / (numAvversari * 2) - 125, 40, 250, 40);
            fichesGiocatore.setBounds((this.getWidth() * (2 * index + 1)) / (numAvversari * 2) - 125, 80, 250, 40);
        } else { // giocatore locale
            nomeGiocatore.setBounds(this.getWidth() / 4 - 175, 3 * this.getHeight() / 4 - 60, 250, 40);
            fichesGiocatore.setBounds(this.getWidth() / 4 - 175, 3 * this.getHeight() / 4 - 20, 250, 40);
        }

        sfondo.add(nomeGiocatore);
        sfondo.add(fichesGiocatore);
        sfondo.repaint();
    }

    // stampa il valore della mano del giocatore passato, già correttamente posizionato
    private JLabel stampaValoreMano(GiocatoreOnline giocatore) {
        int numAvversari = controller.getGiocatori().size() - 1;
        int index = controller.getGiocatori().indexOf(giocatore);
        if (index > controller.getGiocatori().indexOf(controller.getGiocatoreLocale())) {
            index--;
        }

        JLabel valoreManoGiocatore = new JLabel("Valore mano:   " + giocatore.getValoreMano());
        Font font = new Font("Player", Font.BOLD, 25);
        valoreManoGiocatore.setFont(font);
        valoreManoGiocatore.setForeground(Color.black);

        if (giocatore != controller.getGiocatoreLocale()) { // avversari
            valoreManoGiocatore.setBounds((this.getWidth() * (2 * index + 1)) / (numAvversari * 2) - 125, 120, 350, 40);
        } else { // giocatore locale
            valoreManoGiocatore.setBounds(this.getWidth() / 4 - 175, 3 * this.getHeight() / 4 + 20, 350, 40);
        }

        sfondo.add(valoreManoGiocatore);
        sfondo.repaint();

        return valoreManoGiocatore;
    }

    // stampa la carta "carta" a x, y
    private JLabel stampaCarta(int x, int y, String carta) {
        JLabel card = new JLabel(caricaImmagine("dominio/immagini/mazzo/" + carta + ".png"));

        card.setBounds(x, y, 75, 113);

        sfondo.add(card);
        sfondo.repaint();

        return card;
    }

    // stampa i bottoni e il campo di testo per permettere al giocatore di puntare quanto vuole ( minimo 1, massimo ALL IN )
    private void stampaPuntataPlayer() {
        puntataStr = null;
        JButton punta = new JButton(caricaImmagine("dominio/immagini/punta.png"));
        JButton allIN = new JButton(caricaImmagine("dominio/immagini/all_in.png"));
        puntata = new JTextField();

        punta.setBounds(1060, 500, 140, 56);
        allIN.setBounds(1060, 570, 140, 56);
        puntata.setBounds(900, 500, 140, 56);

        Font font = new Font("Puntata", Font.BOLD, 25);
        puntata.setFont(font);
        puntata.setForeground(Color.black);

        punta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                puntataStr = puntata.getText();
            }
        ;
        });
        
        allIN.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                puntataStr = String.valueOf(controller.getGiocatoreLocale().getFiches());
            }
        ;
        });
        
        sfondo.add(punta);
        sfondo.add(allIN);
        sfondo.add(puntata);
        sfondo.repaint();

        while (puntataStr == null) {
            pausa(100);
        }

        sfondo.remove(punta);
        sfondo.remove(allIN);
        sfondo.remove(puntata);
        sfondo.repaint();

        controller.riceviEventoDaVista(new SetPuntata(puntataStr));

        pausa(pausa_breve);
    }

    // stampa i bottoni stai e carta per permettere al giocatore di scegliere la mossa, nel caso di carta stampa la carta
    private void stampaGiocataPlayer() {
        giocataStr = null;
        Carta lastCard;
        if (controller.getGiocatoreLocale().getNumeroCarteScoperte() != 0) {
            lastCard = controller.getGiocatoreLocale().getUltimaCartaOttenuta();
            int index = controller.getGiocatoreLocale().getNumeroCarteScoperte() - 1;
            stampaCarta(this.getWidth() / 2 - 95 + index * 35, 3 * this.getHeight() / 4 - 60, lastCard.toString());
        }
        aggiornaValoreManoPlayer();
        
        sfondo.add(carta);
        sfondo.add(stai);
        sfondo.repaint();

        while (giocataStr == null) {
            pausa(100);
        }

        controller.riceviEventoDaVista(new SetGiocata(giocataStr));

        sfondo.remove(carta);
        sfondo.remove(stai);
        sfondo.repaint();
    }

    // stampa il mazzo che viene rimescolato con relativo messaggio
    private void rimescoloMazzo() {
        ArrayList<JLabel> carte = new ArrayList<>();
        Font font = new Font("Deck Shuffle", Font.BOLD, 55);
        JLabel rimescoloMsg = new JLabel("Rimescolo il mazzo...");
        rimescoloMsg.setFont(font);
        rimescoloMsg.setForeground(Color.black);
        int strWidth = rimescoloMsg.getFontMetrics(font).stringWidth("Rimescolo il mazzo...");
        rimescoloMsg.setBounds(this.getWidth() / 2 - strWidth / 2, this.getHeight() / 2 + 10, strWidth, 90);

        sfondo.add(rimescoloMsg);
        sfondo.repaint();

        try {
            audio.ferma("soundTrack");
            audio.riproduci("deckShuffle");
        } catch (CanzoneNonTrovataException ex) {
            ex.printStackTrace();
        }

        for (int i = 0; i < 12; i++) {
            carte.add(stampaCartaMobileDeckShuffle(this.getWidth() / 2 - 187 - i * 5, this.getHeight() / 2 - 100, "retroCarta", i));
        }

        pausa(pausa_breve);

        try {
            audio.riavviaInLoop("soundTrack");
            audio.riavvolgi("deckShuffle");
        } catch (CanzoneNonTrovataException ex) {
            ex.printStackTrace();
        }

        sfondo.remove(rimescoloMsg);
        for (JLabel cartaTmp : carte) {
            sfondo.remove(cartaTmp);
        }
        sfondo.repaint();
    }

    // serve solo come oggetto del deck shuffle
    private JLabel stampaCartaMobileDeckShuffle(int x, int y, String carta, int spazio) {
        JLabel card = new JLabel(caricaImmagine("dominio/immagini/mazzo/" + carta + ".png"));

        card.setBounds(x, y, 75, 113);

        sfondo.add(card);
        sfondo.repaint();

        for (int i = 0; i < ((this.getWidth() / 2 + 20) - x - 8 * spazio); i++) {
            card.setLocation(x + i, y);
            pausa(1);
            sfondo.repaint();
        }
        return card;
    }

    // stampa l'ultima carta del giocatore in seguito a una mano particolare ( sballato, 7 e mezzo o 7 e mezzo reale )
    private void stampaStatoPlayer() {
        Carta lastCard = controller.getGiocatoreLocale().getUltimaCartaOttenuta();
        int index = controller.getGiocatoreLocale().getNumeroCarteScoperte() - 1;
        stampaCarta(this.getWidth() / 2 - 95 + index * 35, 3 * this.getHeight() / 4 - 60, lastCard.toString());

        sfondo.remove(valoriMano.get(controller.getGiocatoreLocale().getNome()));
        valoriMano.remove(controller.getGiocatoreLocale().getNome());
        valoriMano.put(controller.getGiocatoreLocale().getNome(), stampaStato(controller.getGiocatoreLocale()));
        pausa(pausa_breve);
    }

    private void stampaStatoAvversario(String nome) {
        GiocatoreOnline giocatore = getGiocatore(nome);
        Carta lastCard = giocatore.getUltimaCartaOttenuta();
        int indexCarta = giocatore.getNumeroCarteScoperte() - 1;
        int indexGioc = controller.getGiocatori().indexOf(giocatore);
        stampaCarta((this.getWidth() * (2 * indexGioc + 1)) / ((controller.getGiocatori().size() - 1) * 2) - 95 + indexCarta * 35, 180, lastCard.toString());

        if (valoriMano.get(nome) != null) {
            sfondo.remove(valoriMano.get(nome));
            valoriMano.remove(nome);
        }
        valoriMano.put(nome, stampaStato(getGiocatore(nome)));
        scopriCartaCoperta(giocatore);
        pausa(pausa_breve);
    }

    // stampa la carta coperta degli avversari non visibile e quella del giocatore locale visibile
    private void stampaCartaCoperta() {
        int nGiocatori = controller.getGiocatori().size();
        sfondo.removeAll();
        sfondo.add(esci);
        sfondo.repaint();

        if (!mazziereEstratto) {  // serve per far in modo che anche il 3° e 4° giocatore vedano il mazziere in arancione
            needToMarkMazziere = true;
            mazziereEstratto = true;
        }

        for (int i = 0; i < nGiocatori; i++) {
            stampaNomeFiches(controller.getGiocatori().get(i));
        }

        pausa(pausa_breve);

        Font font = new Font("CarteCoperteMsg", Font.BOLD, 70);
        JLabel messaggioCartaCoperta = new JLabel("Distribuisco carta coperta...");
        messaggioCartaCoperta.setFont(font);
        messaggioCartaCoperta.setForeground(Color.black);
        int strWidth = messaggioCartaCoperta.getFontMetrics(font).stringWidth("Distribuisco carta coperta...");
        messaggioCartaCoperta.setBounds(this.getWidth() / 2 - strWidth / 2, this.getHeight() / 2 - 60, strWidth, 90);

        sfondo.add(messaggioCartaCoperta);
        sfondo.repaint();

        int indexAvversario = 0;

        for (int i = 0; i < nGiocatori; i++) {
            if (controller.getGiocatori().get(i) != controller.getGiocatoreLocale()) {
                carteCoperteAvversari.add(stampaCarta((this.getWidth() * (2 * indexAvversario + 1)) / ((nGiocatori - 1) * 2) - 125, 180, "retroCarta"));
                indexAvversario++;
            } else {
                stampaCarta(this.getWidth() / 2 - 125, 3 * this.getHeight() / 4 - 60, controller.getGiocatoreLocale().getCartaCoperta().toString());
                valoriMano.put(controller.getGiocatoreLocale().getNome(), stampaValoreMano(controller.getGiocatoreLocale()));
            }
            pausa(pausa_breve);
        }

        sfondo.remove(messaggioCartaCoperta);
        sfondo.repaint();

        needCartaCoperta = false;
    }

    // stampa la carta che l'avversario ha appena pescato
    private void stampaGiocatoreHaPescato(GiocatoreOnline giocatore) {
        String nomeGioc = giocatore.getNome();
        int indexGioc = controller.getGiocatori().indexOf(giocatore);
        if (indexGioc > controller.getGiocatori().indexOf(controller.getGiocatoreLocale())) {
            indexGioc--;
        }
        int indexCarta = giocatore.getNumeroCarteScoperte() - 1;

        if (valoriMano.get(nomeGioc) == null) {
            valoriMano.put(nomeGioc, stampaValoreManoAttualeAvversario(giocatore));
        } else {
            aggiornaValoreManoAttualeAvversario(giocatore);
        }
        Carta lastCard = giocatore.getUltimaCartaOttenuta();
        stampaCarta((this.getWidth() * (2 * indexGioc + 1)) / ((controller.getGiocatori().size() - 1) * 2) - 95 + indexCarta * 35, 180, lastCard.toString());
    }

    // stampa il valore mano dell'avversario
    private JLabel stampaValoreManoAttualeAvversario(GiocatoreOnline giocatore) {
        int nGioc = controller.getGiocatori().size() - 1;
        int index = controller.getGiocatori().indexOf(giocatore);
        if (index > controller.getGiocatori().indexOf(controller.getGiocatoreLocale())) {
            index--;
        }
        double valoreMano = 0;

        try {
            valoreMano = giocatore.getValoreMano() - giocatore.getCartaCoperta().getValoreNumerico();
        } catch (MattaException ex) {
            // pass
        }

        JLabel valoreManoGiocatore = new JLabel("Valore attuale:   " + valoreMano);

        Font font = new Font("Player", Font.BOLD, 25);
        valoreManoGiocatore.setFont(font);
        valoreManoGiocatore.setForeground(Color.black);

        valoreManoGiocatore.setBounds((this.getWidth() * (2 * index + 1)) / (nGioc * 2) - 125, 120, 350, 40);

        sfondo.add(valoreManoGiocatore);
        sfondo.repaint();

        return valoreManoGiocatore;
    }

    // aggiorna il valore mano dell'avversario
    private void aggiornaValoreManoAttualeAvversario(GiocatoreOnline giocatore) {
        double valoreMano = 0;

        try {
            valoreMano = giocatore.getValoreMano() - giocatore.getCartaCoperta().getValoreNumerico();
        } catch (MattaException ex) {
            valoreMano = 7;
        }

        valoriMano.get(giocatore.getNome()).setText("Valore attuale:   " + valoreMano);
        sfondo.repaint();
    }

    // stampa il messaggio passato
    private void stampaMsg(String msg, int dimensione) {
        Font font = new Font("MsgDaStampare", Font.BOLD, dimensione);
        msgDaStampare = new JLabel(msg);
        msgDaStampare.setFont(font);
        msgDaStampare.setForeground(Color.black);
        int strWidth = msgDaStampare.getFontMetrics(font).stringWidth(msg);
        msgDaStampare.setBounds(this.getWidth() / 2 - strWidth / 2, this.getHeight() / 2 - 60, strWidth, 90);

        sfondo.add(msgDaStampare);
        sfondo.repaint();
    }

    // scopre la carta coperta (usato se sballato o a fine round per vedere il valore a fine round)
    private void scopriCartaCoperta(GiocatoreOnline giocatore) {
        int index = controller.getGiocatori().indexOf(giocatore);
        if (index > controller.getGiocatori().indexOf(controller.getGiocatoreLocale())) {
            index--;
        }

        carteCoperteAvversari.get(index).setIcon(caricaImmagine("dominio/immagini/mazzo/" + giocatore.getCartaCoperta().toString() + ".png"));
        sfondo.repaint();
    }

    // stampa il cambio di stato di un giocatore: SBALLATO in rosso, SEM in ciano e SEMR in magenta
    private JLabel stampaStato(GiocatoreOnline giocatore) {
        int numAvversari = controller.getGiocatori().size() - 1;
        int index = controller.getGiocatori().indexOf(giocatore);
        if (index > controller.getGiocatori().indexOf(controller.getGiocatoreLocale())) {
            index--;
        }
        JLabel stato = null;

        Font font = new Font("StatoMano", Font.BOLD, 25);

        switch (giocatore.getStatoMano()) {
            case Sballato:
                stato = new JLabel("SBALLATO");
                stato.setFont(font);
                stato.setForeground(Color.red);
                break;
            case SetteeMezzo:
                stato = new JLabel("SETTE E MEZZO");
                stato.setFont(font);
                stato.setForeground(Color.cyan);
                try {
                    audio.riproduci("applausiSEM");
                    audio.riavvolgi("applausiSEM");
                } catch (CanzoneNonTrovataException ex) {
                    ex.printStackTrace();
                }
                break;
            case SetteeMezzoReale:
                stato = new JLabel("SETTE E MEZZO REALE");
                stato.setFont(font);
                stato.setForeground(Color.magenta);
                try {
                    audio.riproduci("applausiSEM");
                    audio.riavvolgi("applausiSEM");
                } catch (CanzoneNonTrovataException ex) {
                    ex.printStackTrace();
                }
                break;
            default:
                break;
        }

        if (giocatore != controller.getGiocatoreLocale()) {
            stato.setBounds((this.getWidth() * (2 * index + 1)) / (numAvversari * 2) - 125, 120, 350, 40);
        } else {
            stato.setBounds(this.getWidth() / 4 - 175, 3 * this.getHeight() / 4 + 20, 350, 40);
        }

        sfondo.add(stato);
        sfondo.repaint();

        return stato;
    }

    // ritorna il giocatore dato il nome
    private GiocatoreOnline getGiocatore(String nome) {
        GiocatoreOnline giocatore = null;
        for (GiocatoreOnline gioc : controller.getGiocatori()) {
            if (gioc.getNome().equals(nome)) {
                giocatore = gioc;
            }
        }
        return giocatore;
    }

    // stampa il valore mano a fine round ( solo per gli avversari perchè quello del giocatore è sempre visibile )
    private void stampaValoreManoFineRound(GiocatoreOnline giocatore) {
        if (giocatore != controller.getGiocatoreLocale()) {
            if (giocatore.getStatoMano() == StatoMano.OK) {
                if (valoriMano.get(giocatore.getNome()) == null) {
                    valoriMano.put(giocatore.getNome(), stampaValoreMano(giocatore));
                } else {
                    valoriMano.get(giocatore.getNome()).setText("Valore mano:   " + giocatore.getValoreMano());
                }
                sfondo.repaint();

                pausa(pausa_breve);
            }
        }
    }

    // aggiorna il valore mano del giocatore ( usato ad ogni giocata )
    private void aggiornaValoreManoPlayer() {
        GiocatoreOnline giocatore = controller.getGiocatoreLocale();
        valoriMano.get(giocatore.getNome()).setText("Valore mano:   " + giocatore.getValoreMano());
        sfondo.repaint();
    }

    // stampa per il giocatore passato il messaggio di fine round di vincita o perdita
    private void stampaMessaggioFineRound(GiocatoreOnline giocatore) {
        GiocatoreOnline mazziere = controller.getMazziere();
        String msg = "";

        if (!giocatore.isMazziere()) {
            if (mazziere.getStatoMano() == StatoMano.Sballato) {
                if (giocatore.getStatoMano() == StatoMano.Sballato)
                    msg = giocatore.getNome() + " paga " + giocatore.getPuntata() + " al mazziere";
                else if ((giocatore.getStatoMano() == StatoMano.OK) || (giocatore.getStatoMano() == StatoMano.SetteeMezzo))
                    msg = giocatore.getNome() + " riceve " + giocatore.getPuntata() + " dal mazziere";
                else if (giocatore.getStatoMano() == StatoMano.SetteeMezzoReale)
                    msg = giocatore.getNome() + " riceve " + 2 * giocatore.getPuntata() + " dal mazziere";
            } else {
                if ((giocatore.getStatoMano() == StatoMano.Sballato) || (giocatore.getValoreMano() <= mazziere.getValoreMano()))
                    msg = giocatore.getNome() + " paga " + giocatore.getPuntata() + " al mazziere";
                else if (giocatore.getValoreMano() > mazziere.getValoreMano())
                    msg = giocatore.getNome() + " riceve " + giocatore.getPuntata() + " dal mazziere";
                else if ((giocatore.getStatoMano() == StatoMano.SetteeMezzoReale) && (giocatore.getValoreMano() > mazziere.getValoreMano()))
                    msg = giocatore.getNome() + " riceve " + 2 * giocatore.getPuntata() + " dal mazziere";
                else if(mazziere.getStatoMano() == StatoMano.SetteeMezzoReale)
                    msg = giocatore.getNome() + " paga " + 2*giocatore.getPuntata() + " al mazziere";
            }
        } else
            msg = "Il mazziere regola i suoi conti";

        Font font = new Font("MsgFineRound", Font.BOLD, 70);
        JLabel msgFineRound = new JLabel(msg);
        msgFineRound.setFont(font);
        msgFineRound.setForeground(Color.black);
        int strWidth = msgFineRound.getFontMetrics(font).stringWidth(msg);
        msgFineRound.setBounds(this.getWidth() / 2 - strWidth / 2, this.getHeight() / 2 - 60, strWidth, 90);

        sfondo.add(msgFineRound);
        sfondo.repaint();

        pausa(pausa_lunga);

        sfondo.remove(msgFineRound);
    }

    // controlla la fine effettiva del round per tutti i giocatori e resetta carte coperte e valori mano
    private void checkFineRound(GiocatoreOnline giocatore) {
        int nGioc = controller.getGiocatori().size();
        int indexGioc = controller.getGiocatori().indexOf(giocatore);
        if (indexGioc == (nGioc - 1)) {
            controllaUscita();
            sfondo.remove(esci);
            carteCoperteAvversari.clear();
            valoriMano.clear();
            sfondo.repaint();
        }
    }

    // stoppa il thread per tempo ms
    private void pausa(int tempo) {
        try {
            Thread.sleep(tempo);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    // controlla se il giocatore ha premuto il bottone esci, se si chiude la partita e torna al menù principale
    private void controllaUscita() {
        if (esciAFineRound) {
            try {
                audio.ferma("soundTrack");
                audio.riavvolgi("soundTrack");
            } catch (CanzoneNonTrovataException ex) {
                Logger.getLogger(PartitaOnlineGuiView.class.getName()).log(Level.SEVERE, null, ex);
            }            
            controller.esci();
            new MenuPrincipaleGui();
            dispose();
        }
    }
}
