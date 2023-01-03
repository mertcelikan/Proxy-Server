package deneme;



import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;

// GET
// curl -v -x http://127.0.0.1:8080 \
// -H 'Connection: close' \
// http://neverssl.com

// POST
// curl -v -x http://127.0.0.1:8080 \
// -X POST
// -H 'Content-Type: application/json' \
// -H 'Connection: close' \
// -d '{"name": "YOURNAME", "surname": "YOURSURNAME"}' \
// http://httpbin.org/post


public class ProxyServer {

	public static HashMap<String, CacheElement> cache = new HashMap<>();
	final static ArrayList<String> strTotal = new ArrayList<>();
	public final static ArrayList<String> forbiddenAddresses = new ArrayList<>();

	static boolean wait = true;
	

	public static void main(String[] args) throws Exception {
		
		
		final JFrame frame = new JFrame("Network Transparent Proxy Project");
		
		JMenuBar menubar =new JMenuBar();  
		final JLabel label = new JLabel("Proxy Server not running.", JLabel.CENTER) ;
		JPanel p = new JPanel();
		p.add(label);
		frame.add(p);
		
		JMenu menu=new JMenu("File");  
		JMenu menu2=new JMenu("Help");  
		
        JMenuItem i1=new JMenuItem("Start");  
        JMenuItem i2=new JMenuItem("Stop");  
        JMenuItem i3=new JMenuItem("Report");  
        JMenuItem i4=new JMenuItem("Add host to filter");  
        JMenuItem i5=new JMenuItem("Display current filtered hosts"); 
        JMenuItem i6=new JMenuItem("Exit"); 
        
        menu.add(i1); menu.add(i2); menu.add(i3); menu.add(i4);menu.add(i5);menu.add(i6);    
         
        menubar.add(menu);
        menubar.add(menu2);
        frame.setJMenuBar(menubar);  
    
		
		
    	
	    //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    
	 
	     // Adds Button to content pane of frame
	    
	    
	    
	    i1.addActionListener((ActionListener) new ActionListener() {
    		public void actionPerformed(ActionEvent e)
    		{
    			
    			label.setText("Proxy Server running !");
    			SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                    	wait = true;
                    	System.out.println("Server Running");            
                    	ServerSocket s = new ServerSocket(8080);
                    	while (true) {
                			Socket clientSocket = s.accept();
                			new ServerHandler(clientSocket).start();
                		}                  
                    }
                };
                worker.execute();	

            };    		
    	});
	    
	    i2.addActionListener((ActionListener) new ActionListener() {
    		public void actionPerformed(ActionEvent e)
    		{
    			label.setText("Proxy Server stopped !");
    			SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                    	wait = false;
                        return null;
                    }

                };
                worker.execute();

    		}
    	});
	    
      	i3.addActionListener((ActionListener) new ActionListener() {
    		public void actionPerformed(ActionEvent e)
    		{
    			JFrame frame5 = new JFrame("Reports");
    			String[] columnNames = {"Date","Client IP","Requested Domain","Path","Method","Status"};
    	        DefaultTableModel model = new DefaultTableModel();
    	        String data;
    	        JTable j = new JTable(model);


    	        model.addColumn("Date");
    	        model.addColumn("Client IP");
    	        model.addColumn("Requested Domain");
    	        model.addColumn("Path");
    	        model.addColumn("Method");   	        
    	        model.addColumn("Status");
    	        model.setColumnIdentifiers(columnNames);
    	        JScrollPane sp = new JScrollPane(j);
    	        for (int j1 = 0; j1<strTotal.size(); j1++)
    	        {
        	        int i = 0;

    	            data = strTotal.get(j1);
    	            String parts[] = data.split("\\|");

    	            model.addRow(new Object[] {parts[0],parts[1],parts[2],parts[3],parts[4],parts[5]});



         		}
    	        FileWriter file = null;
				try {
					file = new FileWriter("report.txt");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    	        PrintWriter output = new PrintWriter(file);


    	        for (int row = 0; row < j.getRowCount(); row++) {
    	            for (int col = 0; col < j.getColumnCount(); col++) {
    	            	output.print(j.getColumnName(col));
    	            	output.print(": ");
    	            	output.println(j.getValueAt(row, col));
    	            }
	            	output.println("----------------------");

    	        }

    	        output.close();
    	        frame5.add(sp);
    	        frame5.setSize(500,500);
    	        frame5.setVisible(true);



    		}
    	});
      	
      	i4.addActionListener((ActionListener) new ActionListener() {
    		public void actionPerformed(ActionEvent e)
    		{
    			String name=JOptionPane.showInputDialog(frame,"Enter web domain name for forbidding");
    			if(name.contains("https://"))
    			{
    				if(!name.contains("www"))
    				{
    					String newStr = name.substring(8);
            			forbiddenAddresses.add("www."+newStr);
    				}
    				String newStr = name.substring(8);
        			forbiddenAddresses.add(newStr);

    			}
    			else if(name.contains("http://"))
    			{
    				if(!name.contains("www"))
    				{
    					String newStr = name.substring(7);
            			forbiddenAddresses.add("www."+newStr);
    				}
    				String newStr = name.substring(7);
        			forbiddenAddresses.add(newStr);
    			}
    			else
    			{
    				if(!name.contains("www"))
    				{

            			forbiddenAddresses.add("www."+name);
    				}
    				else
    				{
        				forbiddenAddresses.add(name);

    				}
    			}

    			 JOptionPane.showMessageDialog(frame,"Successfully Updated.","Alert",JOptionPane.WARNING_MESSAGE);

    		}
    	});
      	
      	i5.addActionListener((ActionListener) new ActionListener() {
    		public void actionPerformed(ActionEvent e)
    		{
    				if(forbiddenAddresses.size()< 1)
    				{
    	    			 JOptionPane.showMessageDialog(frame,"List is Empty","Alert",JOptionPane.WARNING_MESSAGE);

    				}
    				else
    				{
    					JFrame tmpFrame = new JFrame("Forbidden Address List");
            	        String[] columnNames = {"Forbidden Address"};
            	        DefaultTableModel model = new DefaultTableModel();
            	        String data;
            	        JTable j = new JTable(model);
            	        model.addColumn("Forbidden Address");
            	        for(int i = 0; i < forbiddenAddresses.size(); i++)
            	        {
            	            data = forbiddenAddresses.get(i);

            	        	model.insertRow(0, new Object[] {data});
            	        }
            	        JScrollPane sp = new JScrollPane(j);
            	        tmpFrame.add(sp);
            	        tmpFrame.setSize(500,500);
            	        tmpFrame.setVisible(true);
    				}


    		}
    	});
      	
      	i6.addActionListener((ActionListener) new ActionListener() {
    		public void actionPerformed(ActionEvent e)
    		{
    			System.exit(1);
    		}
    	});
      	
      	frame.add(label, JLabel.CENTER);
      	frame.setSize(500,500);
      	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      	frame.setVisible(true);
		

		//while (true) {
			//Socket clientSocket = s.accept();
			//new ServerHandler(clientSocket).start();
		//}
	}

	
}