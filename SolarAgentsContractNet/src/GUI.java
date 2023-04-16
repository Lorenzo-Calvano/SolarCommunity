import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class GUI extends JFrame implements ActionListener {

    private SolarAgent myAgent;

    //Principal Panel
    private JPanel Content = new JPanel(new BorderLayout());

    //panels for populating the content of the frame
    private JPanel Center =  new JPanel();
    private JScrollPane ScrollCenter;
    private JPanel North = new JPanel();
    private JPanel South = new JPanel();

    //buttons for the graphical show
    private JRadioButton Trade = new JRadioButton("Scambi Attivi");
    private JRadioButton Balance = new JRadioButton("Bilancio");
    private JRadioButton ContractNet = new JRadioButton("Contract Net");

    //list for contract net exchanges
    private ArrayList<String> messages = new ArrayList<>();

    //to format output
    private DecimalFormat format = new DecimalFormat();



    public GUI(SolarAgent Solar, int x, int y){
        myAgent = Solar;

        //set format for decimal numbers
        format.setMaximumFractionDigits(3);

        //set frame, position etc...
        this.setBounds(x, y, 330,380);
        this.setTitle("Agent " + myAgent.getLocalName());

        //set interactions for buttons
        Trade.addActionListener(this);
        Balance.addActionListener(this);
        ContractNet.addActionListener(this);

        //add initial elements to sub-panels
        ButtonGroup BG = new ButtonGroup();
        BG.add(Trade);
        BG.add(Balance);
        BG.add(ContractNet);
        this.South.add(Trade);
        this.South.add(Balance);
        this.South.add(ContractNet);

        //show initial interface
        this.show_EnergyTrades();
        this.Trade.setSelected(true);
        this.setContentPane(this.Content);
        this.setVisible(true);
    }

    public void updateGUI(){
        synchronized (myAgent) {
            if (this.Balance.isSelected()) {
                this.show_balance();
            } else if (this.Trade.isSelected()) {
                this.show_EnergyTrades();
            }
        }
        this.setVisible(true);
    }

    public synchronized void update_Contracts(String message){
        if(this.ContractNet.isSelected()){
            JPanel p = new JPanel();
            p.setAlignmentX(Component.CENTER_ALIGNMENT);
            p.add(new JLabel(message));
            this.Center.add(p);

            this.setVisible(true);
        }
        messages.add(message);
    }


    private void show_Contracts(){
        //show list of contract net that took place, with messages exchanged
        this.setNorthPanel("Contract Net");
        this.setCenterPanel_Contract();

        this.Content.removeAll();
        this.ScrollCenter = new JScrollPane(this.Center);

        this.Content.add(this.North, BorderLayout.NORTH);
        this.Content.add(this.ScrollCenter, BorderLayout.CENTER);
        this.Content.add(this.South, BorderLayout.SOUTH);
    }

    private void show_balance(){
        //show state by energy balance
        if(myAgent.getEnergyBalance() < 0)
            this.setNorthPanel("Stato di Ricezione Energetica");
        else if(myAgent.getEnergyBalance() == 0)
            this.setNorthPanel("Stato di Equilibrio Energetico");
        else
            this.setNorthPanel("Stato di Invio Energetico");

        this.setCenterPanel_Energy();

        this.Content.removeAll();
        this.ScrollCenter = new JScrollPane(this.Center);
        this.Content.add(this.North, BorderLayout.NORTH);
        this.Content.add(this.ScrollCenter, BorderLayout.CENTER);
        this.Content.add(this.South, BorderLayout.SOUTH);
    }

    private void show_EnergyTrades(){
        //show current energy received
        this.setCenterPanel_Trade();
        this.setNorthPanel("Scambi Energetici");

        this.ScrollCenter = new JScrollPane(this.Center);

        this.Content.removeAll();
        this.Content.add(this.ScrollCenter, BorderLayout.CENTER);
        this.Content.add(this.North, BorderLayout.NORTH);
        this.Content.add(this.South, BorderLayout.SOUTH);
    }


    private void setNorthPanel(String phrase1){
        this.North = new JPanel();
        if(phrase1 != null) {
            JLabel JL = new JLabel("<html> <font color=black, size=5> " + phrase1 + " </font> <html>");
            this.North.add(JL);
        }
    }

    private void setCenterPanel_Trade(){
        this.Center = new JPanel();
        String label = "<font size=4>Consumo: " + this.format.format(myAgent.getEnergyConsumed()) + "  -----  " +
                "Produzione: " + this.format.format(myAgent.getEnergyProduced()) + "</font><br>";


        if(myAgent.EnergyReceived.size() != 0) {
            label += "<font size=4> ------------------------- <br> </font>";
            for (EnergyTrade e : myAgent.EnergyReceived) {
                label += ("<font size=5> Io </font><font color='red', size=5>  &lt;&lt;&lt; "+
                        this.format.format(e.getEnergyValue()) + " W &lt;&lt;&lt; </font> <font size=5> " + e.getProcessName() + " </font>  <br> ");
            }
            label += "<font size=4> -------------------------</font><br>";
        }

        if(myAgent.EnergySent.size() != 0){
            label += "<font size=4> ------------------------- <br> </font>";
            for (EnergyTrade e : myAgent.EnergySent) {
                label += ("<font size=5> Io </font><font color='green', size=5>  >>> " +
                        this.format.format(e.getEnergyValue()) + " W >>> </font> <font size=5> " + e.getProcessName() +" </font> <br> ");
            }
            label += "<font size=4> -------------------------</font><br>";
        }

        this.Center.add(new JLabel("<html> <p align=center>" + label + " </p> </html>"));
    }

    private void setCenterPanel_Energy(){
        this.Center = new JPanel();

        double sent = 0;
        for (EnergyTrade e : myAgent.EnergySent) {
            sent += e.getEnergyValue();
        }

        double received = 0;
        for (EnergyTrade e : myAgent.EnergyReceived) {
            received += e.getEnergyValue();
        }

        JLabel text = new JLabel("<html> <p align=center>  <font size=4> ------------------------- <br>" +
                "Consumo: " + this.format.format(myAgent.getEnergyConsumed())+ " W <br>" +
                "Produzione: " + this.format.format(myAgent.getEnergyProduced()) + " W <br> <br>" +
                "Potenza Inviata: " + this.format.format(sent) + " W <br>" +
                "Potenza Ricevuta: " + this.format.format(received) + " W <br> <br>" +
                "Bilancio Energetico Totale: " + this.format.format(myAgent.getEnergyBalance() + received - sent) + " W" +
                "<br> -------------------------</font> </p></html>");
        this.Center.add(text);
    }

    private void setCenterPanel_Contract(){
        this.Center = new JPanel();
        this.Center.setLayout(new BoxLayout(this.Center, BoxLayout.Y_AXIS));

        for (String m : this.messages) {
            JPanel p = new JPanel();
            p.add(new JLabel(m));
            p.setAlignmentX(Component.CENTER_ALIGNMENT);
            this.Center.add(p);
        }

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == this.Balance){
            synchronized (myAgent){
                this.show_balance();
                this.setVisible(true);
            }
        }
        else if(e.getSource() == this.Trade){
            synchronized (myAgent){
                this.show_EnergyTrades();
                this.setVisible(true);
            }
        }
        else if(e.getSource() == this.ContractNet){
            synchronized (myAgent) {
                this.show_Contracts();
                this.setVisible(true);
            }
        }
    }
}
