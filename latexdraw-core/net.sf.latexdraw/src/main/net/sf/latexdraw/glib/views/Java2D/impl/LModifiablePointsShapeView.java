package net.sf.latexdraw.glib.views.Java2D.impl;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;

import net.sf.latexdraw.glib.models.interfaces.IModifiablePointsShape;
import net.sf.latexdraw.glib.models.interfaces.IPoint;
import net.sf.latexdraw.glib.views.Java2D.interfaces.IViewModifiablePtsShape;
import net.sf.latexdraw.util.LNumber;

/**
 * Defines an abstract view of the IModifiablePointsShape model.<br>
 * <br>
 * This file is part of LaTeXDraw.<br>
 * Copyright (c) 2005-2013 Arnaud BLOUIN<br>
 * <br>
 * LaTeXDraw is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 * <br>
 * LaTeXDraw is distributed without any warranty; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.<br>
 * <br>
 * 03/18/2008<br>
 * @author Arnaud BLOUIN
 * @since 3.0
 */
abstract class LModifiablePointsShapeView<S extends IModifiablePointsShape> extends LShapeView<S> implements IViewModifiablePtsShape {
	/**
	 * Initialises the view of a IModifiablePointsShape.
	 */
	protected LModifiablePointsShapeView(final S model) {
		super(model);
	}


	/**
	 * Update the path of the multi-point shape.
	 * @param close True: the shape will be closed.
	 * @since 3.0
	 */
	protected void setPath(final boolean close) {
		final List<IPoint> pts = shape.getPoints();

		path.reset();

		if(pts.size()>0) {
			final int size 	= pts.size();
			IPoint pt		= pts.get(0);
			// We must check if all the points of the shape are equals because if it
			// is the case the shape will not be visible.
			final double firstX = pt.getX();
			final double firstY = pt.getY();
			double sumX	= firstX;
			double sumY	= firstY;

			path.moveTo(firstX, firstY);

			for(int i=1; i<size; i++) {
				pt = pts.get(i);
				path.lineTo(pt.getX(), pt.getY());
				// To check the points position, all the X coordinates are added together
				// and at then end we compare if the first X coordinate equals to the average of the X
				// coordinate (idem from Y coordinates). If it is the case, all the point are equals.
				sumX += pt.getX();
				sumY += pt.getY();
			}

			// Checking the equality between points of the shape.
			if(LNumber.INSTANCE.equals(firstX, sumX/size) && LNumber.INSTANCE.equals(firstY, sumY/size)) {
				// If they are all equals, we draw a tiny but visible line.
				path.reset();
				path.moveTo(firstX, firstY);
				path.lineTo(firstX+1, firstY);
			}
			else
				if(close)
					path.closePath();
		}
	}


	@Override
	public Shape getRotatedShape2D() {
		return path;
	}


	@Override
	protected IPoint beginRotation(final Graphics2D g) {
		return null;
	}


	@Override
	protected void endRotation(final Graphics2D g, final IPoint translation) {
		// Nothing to do.
	}


//	@Override
//	public boolean intersects(final Rectangle2D rec) {
//		if(rec==null || (LNumber.INSTANCE.equals(shape.getRotationAngle(), 0.) && !rec.contains(border) && !border.contains(rec) && !rec.intersects(border)))
//			return false;
//
//		final BasicStroke stroke = getStroke();
//
//		if(stroke==null)
//			return path.intersects(rec) || path.contains(rec);
//
//		return (shape.isFilled() && path.contains(rec)) || stroke.createStrokedShape(path).intersects(rec);
//	}
//
//
//
//	@Override
//	public boolean contains(final double x, final double y) {
//		// We test the borders first to limit the computations.
//		if(!border.contains(x, y))
//			return false;
//
//		if(shape.isFilled() && path.contains(x, y))
//			return true;
//
//		final BasicStroke bc = getStroke();
//
//		// We test if the point is on the shape.
//		return bc==null ? false : bc.createStrokedShape(path).contains(x, y);
//	}
//
//
//	@Override
//	public void updateBorder() {
//		border.setFrame(getStroke().createStrokedShape(path).getBounds2D());
//	}


	@Override
	protected void updateDblePathInside() {
		updateDblePathMiddle();
	}


	@Override
	protected void updateDblePathMiddle() {
		updateGeneralPathMiddle();
	}


	@Override
	protected void updateDblePathOutside() {
		updateDblePathMiddle();
	}


	@Override
	protected void updateGeneralPathInside() {
		updateGeneralPathMiddle();
	}


	@Override
	protected void updateGeneralPathMiddle() {
		setPath(true);
	}


	@Override
	protected void updateGeneralPathOutside() {
		updateGeneralPathMiddle();
	}
}