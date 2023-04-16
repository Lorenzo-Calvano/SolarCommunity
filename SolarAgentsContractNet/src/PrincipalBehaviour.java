import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Collections;

public class PrincipalBehaviour extends CyclicBehaviour {

    private ContractSender CS = new ContractSender();

    @Override
    public void action() {
        //placement for myAgent
        SolarAgent SA = (SolarAgent) myAgent;

        //Sender / Receiver / Balanced states
        if(SA.getEnergyBalance() > 0) {
            //clear all received energy, close trades
            if(SA.EnergyReceived.size() != 0){
                //send messages to close
                synchronized (myAgent){
                    closingDeals(SA.EnergyReceived, ACLMessage.INFORM_IF);
                    SA.EnergyReceived.clear();

                    //set the next step of energy
                    SA.TEC.setEnergyTimeStep();
                    //visualize results
                    SA.gui.updateGUI();
                }
            }

            //receive proposals from others (thread needed)
            this.SenderAction((SolarAgent) myAgent);
        }
        else if (SA.getEnergyBalance() < 0){

            //clear all sent energy
            if(SA.EnergySent.size() != 0){
                //send messages to empty the energy sent
                synchronized (myAgent) {
                    closingDeals(SA.EnergySent, ACLMessage.INFORM);
                    SA.EnergySent.clear();

                    //set next energy step
                    SA.TEC.setEnergyTimeStep();
                    //visualize the update
                    SA.gui.updateGUI();
                }
            }

            //ask for energy (if needed)
            this.ReceiverAction((SolarAgent) myAgent);
        }
        else{
            //close trades opened before and calculate energy used (if any)
            this.BalancedAction((SolarAgent) myAgent);
        }

        //to block things (diminish it to see better results for agents)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

    }


    //actions for states of agent
    private void ReceiverAction(SolarAgent SA){

        double EB = SA.computeReceiverBalance();
        //based on balance, do what is needed in receiving state
        if(EB<0){
            System.out.println(SA.getLocalName() + ": Devo ricevere");

            //request for values using Contract Net:
            this.CS.searchEnergyExchange(EB, (SolarAgent) myAgent);

        }
        else if(EB>0){
            System.out.println(myAgent.getLocalName() + ": Devo abbassare un valore di scambio");
            synchronized (myAgent) {
                //change the list
                ArrayList<EnergyTrade> tmp = new ArrayList<>();
                //sort to order values from greater to smaller
                Collections.sort(SA.EnergyReceived, Collections.reverseOrder(EnergyTrade::compareTo));
                EB = SA.computeReceiverBalance();
                for (EnergyTrade E : SA.EnergyReceived) {
                    if (EB < E.getEnergyValue() && EB != 0.0) {
                        E.setEnergyValue(E.getEnergyValue() - EB);
                        tmp.add(E);
                        //message to sender for new exchange
                        this.sendInformToSender(E, 1);

                        EB = 0;
                    }
                    else if(EB != 0.0){
                        //the new value is zero, we need to lower  the balance and remove the element
                        EB -= E.getEnergyValue();

                        //message to close exchange (zero value)
                        this.sendInformToSender(E, 0);
                    }
                    else if(EB == 0) {
                        tmp.add(E);
                    }
                }

                //set new array
                SA.EnergyReceived = tmp;
                //energy change --> set the value
                SA.TEC.setEnergyTimeStep();
                //visualize differences
                SA.gui.updateGUI();
            }

        }

    }

    private void SenderAction(SolarAgent SA){
        double EB = SA.computeSenderBalance();
        //based on balance, do what is needed in sender state
        if(EB<0){
            System.out.println(myAgent.getLocalName() + ": Devo abbassare un valore di scambio");

            synchronized (myAgent) {
                //change the list
                ArrayList<EnergyTrade> tmp = new ArrayList<>();
                //sort from greatest to smallest
                Collections.sort(SA.EnergyReceived, Collections.reverseOrder(EnergyTrade::compareTo));
                EB = SA.computeSenderBalance();
                for (EnergyTrade E : SA.EnergySent) {
                    if (-EB < E.getEnergyValue() && EB != 0.0) {
                        E.setEnergyValue(E.getEnergyValue() + EB);

                        //diminish the trade
                        this.sendInformToReceiver(E, 1);

                        tmp.add(E);
                        EB = 0;
                    }
                    else if(EB != 0.0){
                        //close the exchange
                        this.sendInformToReceiver(E, 0);

                        //update the value of balance and remove the trade from array
                        EB += E.getEnergyValue();
                    }
                    else if (EB == 0) {
                        tmp.add(E);
                    }
                }

                SA.EnergySent = tmp;
                //energy change --> set the value
                SA.TEC.setEnergyTimeStep();
                //visualize the results
                SA.gui.updateGUI();
            }
        }

    }

    private void BalancedAction(SolarAgent SA){
        //close trades (if any)
        if(SA.EnergyReceived.size() != 0){
            synchronized (myAgent) {
                closingDeals(SA.EnergyReceived, ACLMessage.INFORM_IF);
                SA.EnergyReceived.clear();
                //set next energy step
                SA.TEC.setEnergyTimeStep();
                //visualize change
                SA.gui.updateGUI();
            }
        }

        if(SA.EnergySent.size() != 0){
            synchronized (myAgent){
                closingDeals(SA.EnergySent, ACLMessage.INFORM);
                SA.EnergySent.clear();
                //set next energy step
                SA.TEC.setEnergyTimeStep();
                //visualize change
                SA.gui.updateGUI();
            }
        }
    }


    //helper functions for informing sender (or receiver) of changes in the trade
    private void sendInformToReceiver(EnergyTrade E, int check){
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        inform.addReceiver(new AID(E.getProcessName(), AID.ISLOCALNAME));

        //new trade of energy between agents (attention on same agent multiple time --> check it)
        EnergyTrade change;

        //based on check decide if closing or diminishing the trade
        if(check == 1)
            change = new EnergyTrade(myAgent.getLocalName(), E.getEnergyValue(),0.0);
        else
            change = new EnergyTrade(myAgent.getLocalName(), 0.0, 0.0);

        inform.setContent(change.toString());
        myAgent.send(inform);
    }

    private void sendInformToSender(EnergyTrade E, int check){
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM_IF);
        inform.addReceiver(new AID(E.getProcessName(), AID.ISLOCALNAME));

        //new trade of energy between agents (attention on same agent multiple time --> check it)
        EnergyTrade change;
        //if lower trade is needed
        if(check == 1){
            change = new EnergyTrade(myAgent.getLocalName(), E.getEnergyValue(), 0.0);
        }
        //if it is necessary to close the trade
        else{
            change = new EnergyTrade(myAgent.getLocalName(), 0.0, 0.0);
        }

        inform.setContent(change.toString());
        myAgent.send(inform);
    }


    //helper function to close all existing trades
    private void closingDeals(ArrayList<EnergyTrade> Trades, int type){
        //close the deals, based on type of trade
        for (EnergyTrade E : Trades) {
            AID Receiver = new AID(E.getProcessName(), AID.ISLOCALNAME);
            ACLMessage msg = new ACLMessage(type);
            msg.addReceiver(Receiver);
            EnergyTrade e = new EnergyTrade(myAgent.getLocalName(), 0.0, 0.0);
            msg.setContent(e.toString());
            myAgent.send(msg);
        }
    }
}
