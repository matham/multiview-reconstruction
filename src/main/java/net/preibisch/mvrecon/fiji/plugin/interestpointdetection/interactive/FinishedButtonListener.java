package net.preibisch.mvrecon.fiji.plugin.interestpointdetection.interactive;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public  class FinishedButtonListener implements ActionListener {
	final InteractiveDoG parent;
	final boolean cancel;

	public FinishedButtonListener(
			final InteractiveDoG parent,
			final boolean cancel) {
		this.parent = parent;
		this.cancel = cancel;
	}

	@Override
	public void actionPerformed(final ActionEvent arg0) {
		parent.wasCanceled = cancel;
		parent.dispose();
	}
}
