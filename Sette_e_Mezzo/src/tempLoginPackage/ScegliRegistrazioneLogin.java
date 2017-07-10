package tempLoginPackage;

import comunicazione.Client;
import dominio.gui.Sfondo;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

public class ScegliRegistrazioneLogin extends JFrame {
    
    private Sfondo sfondo;
    private JButton login, regis, recupero, indietro;
    private Client client;
    
    public ScegliRegistrazioneLogin() {
        setTitle("Fai il Login o Registrati !");
        setPreferredSize(new Dimension(1000, 800));
        setMinimumSize(new Dimension(1000, 800));
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        inizializzaGUI();
        
        client= new Client();
    }
    
    public ScegliRegistrazioneLogin(Client client) {
        setTitle("Fai il Login o Registrati !");
        setPreferredSize(new Dimension(1000, 800));
        setMinimumSize(new Dimension(1000, 800));
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        inizializzaGUI();
        
        this.client= client;
    }
    
    private void inizializzaGUI() {
        sfondo = new Sfondo("dominio/immagini/sfondo.png", 995, 765);
        sfondo.setBounds(0, 0, 1000, 800);
        add(sfondo);
                
        login = new JButton(caricaImmagine("dominio/immagini/login.png"));
        regis = new JButton(caricaImmagine("dominio/immagini/registrati.png"));
        recupero = new JButton(caricaImmagine("dominio/immagini/recuperoPw.png"));
        indietro = new JButton(caricaImmagine("dominio/immagini/indietro.png"));
        
        login.setBounds(this.getWidth() / 2 - 100, 150, 200, 80);
        regis.setBounds(this.getWidth() / 2 - 100, 250, 200, 80);
        recupero.setBounds(this.getWidth() / 2 - 100, 350, 200, 80);
        indietro.setBounds(this.getWidth() / 2 - 100, 600, 200, 80);
        
        login.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LoginMenu(client);
                dispose();
            }
        });
        
        regis.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new RegistrazioneMenu(client);
                dispose();
            }
        });
        
        recupero.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new RecuperoPassword(client);
                dispose();
            }
        });
        
        indietro.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client.close();
            }
        });
        
        sfondo.add(login);
        sfondo.add(regis);
        sfondo.add(recupero);
        sfondo.add(indietro);
        sfondo.repaint();
    }
    
    public void addIndietroActionListener(ActionListener l){
        indietro.addActionListener(l);
    }
    
    private ImageIcon caricaImmagine(String nome) {
        ClassLoader loader = getClass().getClassLoader();
        URL percorso = loader.getResource(nome);
        return new ImageIcon(percorso);
    }
}