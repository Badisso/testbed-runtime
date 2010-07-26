/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.gtr.wsngui;

import javax.swing.*;
import java.awt.*;

/**
 * Created by:
 * User: bimschas
 * Date: 04.03.2010
 * Time: 16:16:27
 */
public class ControllerView extends JPanel {

	private ControllerModel controllerModel;

	private JPanel panel;

	private JButton receiveButton;

	private JButton receiveStatusButton;

	private JLabel endpointUrlLabel;

	private JTextField endpointUrlTextField;

	private JPanel superPanel;


	public ControllerView(ControllerModel controllerModel) {

		super(new FlowLayout());
		((FlowLayout) super.getLayout()).setAlignment(FlowLayout.LEFT);

		this.controllerModel = controllerModel;

		this.superPanel = new JPanel(new GridLayout(4, 1));
		this.panel = new JPanel(new GridLayout(3, 2));
		this.superPanel.add(this.panel);

		{
			endpointUrlLabel = new JLabel("Endpoint URL");
			endpointUrlTextField = new JTextField();

			panel.add(endpointUrlLabel);
			panel.add(endpointUrlTextField);
		}
		{
			receiveButton = new JButton("receive()");

			panel.add(new JLabel());
			panel.add(receiveButton);
		}
		{
			receiveStatusButton = new JButton("receiveStatus()");

			panel.add(new JLabel());
			panel.add(receiveStatusButton);
		}

		add(superPanel);

	}

	public JButton getReceiveButton() {
		return receiveButton;
	}

	public JButton getReceiveStatusButton() {
		return receiveStatusButton;
	}

	public JTextField getEndpointUrlTextField() {
		return endpointUrlTextField;
	}

}
