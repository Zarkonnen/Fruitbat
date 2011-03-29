package com.metalbeetle.fruitbat.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import static java.awt.GridBagConstraints.*;

class SimpleProgressPanel2 extends JPanel {
	final JLabel infoL;
	final JProgressBar progressBar;

	public JLabel getInfoLabel() { return infoL; }
	public JProgressBar getProgressBar() { return progressBar; }

	SimpleProgressPanel2() {
		setLayout(new GridBagLayout());
		GridBagConstraints pbcs = new GridBagConstraints(
					     /*gridx*/ 0,
					     /*gridy*/ 0,
					 /*gridwidth*/ 1,
					/*gridheight*/ 1,
					   /*weightx*/ 1,
					   /*weighty*/ 1,
					    /*anchor*/ CENTER,
					      /*fill*/ HORIZONTAL,
					    /*insets*/ new Insets(10, 10, 10, 10),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);
		GridBagConstraints ilcs = new GridBagConstraints(
					     /*gridx*/ 0,
					     /*gridy*/ 1,
					 /*gridwidth*/ 1,
					/*gridheight*/ 1,
					   /*weightx*/ 1,
					   /*weighty*/ 1,
					    /*anchor*/ WEST,
					      /*fill*/ NONE,
					    /*insets*/ new Insets(0, 10, 10, 10),
					     /*ipadx*/ 0,
					     /*ipady*/ 0
					);
		add(progressBar = new JProgressBar(), pbcs);
		add(infoL = new JLabel("..."), ilcs);
			infoL.setFont(infoL.getFont().deriveFont(10.0f));
	}
}
