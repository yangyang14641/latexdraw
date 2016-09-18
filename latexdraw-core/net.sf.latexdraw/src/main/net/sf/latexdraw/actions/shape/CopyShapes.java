/*
 * This file is part of LaTeXDraw.<br>
 * Copyright (c) 2005-2016 Arnaud BLOUIN<br>
 * <br>
 * LaTeXDraw is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 * <br>
 * LaTeXDraw is distributed without any warranty; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.<br>
 * <br>
 * 2012-04-20<br>
 */
package net.sf.latexdraw.actions.shape;

import net.sf.latexdraw.glib.models.ShapeFactory;
import net.sf.latexdraw.glib.models.interfaces.shape.IShape;
import org.malai.action.Action;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This action copies the selected shapes.
 * @author Arnaud BLOUIN
 */
public class CopyShapes extends Action {
	/** The selection action. */
	protected SelectShapes selection;

	/** The copied shapes from the selection. */
	protected List<IShape> copiedShapes;

	/** The number of times that the shapes have been copied. Use to compute the gap while pasting. */
	protected int nbTimeCopied;


	@Override
	protected void doActionBody() {
		copiedShapes = selection.shapes().stream().map(ShapeFactory::duplicate).collect(Collectors.toList());
	}

	@Override
	public boolean cancelledBy(final Action action) {
		return action instanceof CopyShapes;
	}

	@Override
	public boolean isRegisterable() {
		return true;
	}

	@Override
	public boolean canDo() {
		return selection !=null && !selection.shapes().isEmpty();
	}

	/**
	 * @param sel The selected shapes to copy or cut.
	 */
	public void setSelection(final SelectShapes sel) {
		selection = sel;
	}

	public SelectShapes getSelection() {
		return selection;
	}


	@Override
	public void flush() {
		super.flush();
		copiedShapes.clear();
		selection = null;
	}
}
