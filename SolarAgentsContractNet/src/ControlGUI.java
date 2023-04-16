import jade.core.AID;
import jade.gui.GuiEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

public class ControlGUI extends JFrame implements ActionListener {

    private Control_Agent Control;

    //principal content
    private JPanel Stage = new JPanel(new BorderLayout());

    //panels of interface
    private JPanel North = new JPanel();
    private JPanel Center = new JPanel();
    private JScrollPane ScrollCenter;
    private JPanel South = new JPanel();

    //control buttons to manage the simulations
    private JRadioButton Auto = new JRadioButton("Random");
    private JRadioButton Manual = new JRadioButton("Manuale");
    private JRadioButton ScanFile = new JRadioButton("Scanner");

    //manual interface
    private JTextField Input = new JTextField(10);
    private JComboBox<String> Agents = new JComboBox<>();
    private JButton Submit = new JButton("Invia");
    private JRadioButton Consumed = new JRadioButton("Cons");
    private JRadioButton Produced =  new JRadioButton("Prod");

    //scan interface
    public JTextField Files = new JTextField(20);
    private JButton Scan = new JButton("Invia");

    private DecimalFormat format = new DecimalFormat();



    public ControlGUI(Control_Agent CA){
        this.Control = CA;

        //set up the controller interface
        this.setTitle("Controller");
        this.setBounds(1300,500, 400, 440);

        //create the modes of simulations (north panels)
        ButtonGroup BG = new ButtonGroup();
        BG.add(this.Auto);
        BG.add(this.Manual);
        BG.add(this.ScanFile);
        this.Auto.addActionListener(this);
        this.Manual.addActionListener(this);
        this.ScanFile.addActionListener(this);
        this.Scan.addActionListener(this);

        //create elements for south panels
        this.Submit.addActionListener(this);
        for (AID agent: Control.SolarAgents) {
            this.Agents.addItem(agent.getLocalName());
        }

        //populate the panels
        this.North.add(this.Auto);
        this.North.add(this.Manual);
        this.North.add(this.ScanFile);

        this.Center.add(new JLabel("<html><p align=center><font color=black, size=5>Control Agent Interface</font><br> " +
                "<font size=4>Simula il bilancio degli agenti<br>" +
                "cambiando produzione e/o consumo<br>------------------------------<br>" +
                "<font color=black, size=5>Random</font><br> Modifica automaticamente <br> " +
                "il bilancio energetico degli agenti <br>------------------------------<br>" +
                "<font color=black, size=5>Manuale</font><br>Seleziona manualmente i valori con cui<br>" +
                "attivare i sensori degli agenti<br>------------------------------<br>" +
                "<font color=black, size=5>Scanner</font><br>Fornisci file in formato CSV con i<br>" +
                "comandi da impartire agli agenti<br>------------------------------<br></font></p></html>"));

        this.ScrollCenter = new JScrollPane(this.Center);

        //create the stage
        this.Stage.add(this.North, BorderLayout.NORTH);
        this.Stage.add(this.ScrollCenter, BorderLayout.CENTER);
        this.setContentPane(this.Stage);
        this.setVisible(true);

        //set format for numerical values
        this.format.setMaximumFractionDigits(3);
    }

    private void AutoInterface(){
        //remove things to change
        this.Stage.remove(this.ScrollCenter);
        this.Stage.remove(this.South);

        this.Center = new JPanel();
        this.Center.setLayout(new BoxLayout(this.Center, BoxLayout.Y_AXIS));
        this.ScrollCenter = new JScrollPane(this.Center);

        this.Stage.add(this.ScrollCenter, BorderLayout.CENTER);
        this.setContentPane(this.Stage);
        this.setVisible(true);
    }

    private void ManualInterface(){
        //remove things to change
        this.Stage.remove(this.ScrollCenter);
        this.Stage.remove(this.South);

        this.Center = new JPanel();
        this.Center.setLayout(new BoxLayout(this.Center, BoxLayout.Y_AXIS));
        this.ScrollCenter = new JScrollPane(this.Center);

        //create interface for manual interaction
        this.Input.setText("");
        ButtonGroup CP = new ButtonGroup();
        CP.add(this.Consumed);
        CP.add(this.Produced);
        Box vB = Box.createVerticalBox();
        vB.add(this.Consumed);
        vB.add(this.Produced);
        JPanel P = new JPanel();
        P.add(this.Input);
        P.add(vB);
        P.add(this.Agents);
        P.add(this.Submit);
        this.setSouthPanel(P);

        this.Stage.add(this.South, BorderLayout.SOUTH);
        this.Stage.add(this.ScrollCenter, BorderLayout.CENTER);
        this.setContentPane(this.Stage);
        this.setVisible(true);
    }

    private void ScanInterface(){
        //remove things to change
        this.Stage.remove(this.ScrollCenter);
        this.Stage.remove(this.South);

        //create stage for scanning
        JPanel P = new JPanel();
        this.Files.setText("");
        P.add(this.Files);
        P.add(this.Scan);

        this.setSouthPanel(P);
        this.Center = new JPanel();
        this.Center.setLayout(new BoxLayout(this.Center, BoxLayout.Y_AXIS));
        this.ScrollCenter = new JScrollPane(this.Center);

        this.Stage.add(this.South, BorderLayout.SOUTH);
        this.Stage.add(this.ScrollCenter, BorderLayout.CENTER);
        this.setContentPane(this.Stage);
        this.setVisible(true);
    }

    private void setSouthPanel(JPanel p){
        this.South = p;
    }

    public void updateAutoInterface(String message){
        JLabel J = new JLabel(message);
        J.setFont(new Font("Arial", Font.BOLD, 15));
        J.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.Center.add(J);
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //simulation modes: automatic, manual or scan file (first step)
        if(e.getSource() == this.Auto){
            this.AutoInterface();
            //notify agent to start automatic changes
            Control.postGuiEvent(new GuiEvent(this, 1));
        }
        else if(e.getSource() == this.Manual){
            this.ManualInterface();
            //notify agent to stop automatic changes
            Control.postGuiEvent(new GuiEvent(this, 0));
        }
        else if(e.getSource() == this.ScanFile){
            this.ScanInterface();
        }

        //send change in environment to specific agent (manual)
        else if(e.getSource() == this.Submit){
            String txt = this.Input.getText();
            double value = Double.parseDouble(txt);

            if(value < 0.0){
                System.out.println("Valore non valido!");
            }
            else{
                //message to simulate sensor in environment
                if(this.Consumed.isSelected()){
                    Control.send_change(this.Agents.getSelectedItem().toString(), value, "consumed@");
                    this.updateAutoInterface(this.Agents.getSelectedItem().toString() + " --- consumo: " + this.format.format(value));
                }

                else if(this.Produced.isSelected()){
                    Control.send_change(this.Agents.getSelectedItem().toString(), value, "produced@");
                    this.updateAutoInterface(this.Agents.getSelectedItem().toString() + " --- produzione: " + this.format.format(value));
                }
                this.setVisible(true);

            }
        }
        //scan selection
        else if(e.getSource() == this.Scan){
            String filename = this.Files.getText();
            //notify agent to start behaviour of scanner
            Control.postGuiEvent(new GuiEvent(this, 2));
        }
    }
}
