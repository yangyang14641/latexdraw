/*
 * This file is part of LaTeXDraw.
 * Copyright (c) 2005-2017 Arnaud BLOUIN
 * LaTeXDraw is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 * LaTeXDraw is distributed without any warranty; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 */
package net.sf.latexdraw.instruments;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;
import net.sf.latexdraw.actions.shape.AddShape;
import net.sf.latexdraw.actions.shape.InitTextSetter;
import net.sf.latexdraw.actions.shape.InsertPicture;
import net.sf.latexdraw.models.MathUtils;
import net.sf.latexdraw.models.ShapeFactory;
import net.sf.latexdraw.models.interfaces.shape.BorderPos;
import net.sf.latexdraw.models.interfaces.shape.IBezierCurve;
import net.sf.latexdraw.models.interfaces.shape.IControlPointShape;
import net.sf.latexdraw.models.interfaces.shape.IFreehand;
import net.sf.latexdraw.models.interfaces.shape.IGroup;
import net.sf.latexdraw.models.interfaces.shape.IModifiablePointsShape;
import net.sf.latexdraw.models.interfaces.shape.IPoint;
import net.sf.latexdraw.models.interfaces.shape.IPolygon;
import net.sf.latexdraw.models.interfaces.shape.IPolyline;
import net.sf.latexdraw.models.interfaces.shape.IPositionShape;
import net.sf.latexdraw.models.interfaces.shape.IRectangularShape;
import net.sf.latexdraw.models.interfaces.shape.IShape;
import net.sf.latexdraw.models.interfaces.shape.ISquaredShape;
import net.sf.latexdraw.util.Inject;
import net.sf.latexdraw.util.LangTool;
import net.sf.latexdraw.view.jfx.ViewFactory;
import net.sf.latexdraw.view.jfx.ViewShape;
import org.malai.interaction.Interaction;
import org.malai.javafx.binding.JfXWidgetBinding;
import org.malai.javafx.interaction.JfxInteraction;
import org.malai.javafx.interaction.library.AbortableDnD;
import org.malai.javafx.interaction.library.MultiClick;
import org.malai.javafx.interaction.library.Press;
import org.malai.stateMachine.MustAbortStateMachineException;

/**
 * This instrument allows to draw shapes.
 * @author Arnaud Blouin
 */
public class Pencil extends CanvasInstrument {
	/** The current editing choice (rectangle, ellipse, etc.) of the instrument. */
	protected EditionChoice currentChoice;

	@Inject protected TextSetter textSetter;

	private FileChooser pictureFileChooser;

	private IGroup groupParams;


	public Pencil() {
		super();
		currentChoice = EditionChoice.RECT;
	}

	FileChooser getPictureFileChooser() {
		if(pictureFileChooser == null) {
			pictureFileChooser = new FileChooser();
			pictureFileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter(
				LangTool.INSTANCE.getBundle().getString("Filter.1"), Arrays.asList("*.png", "*.bmp", "*.gif", "*.jpeg", "*.jpg")));
		}
		return pictureFileChooser;
	}

	public IGroup getGroupParams() {
		if(groupParams == null) {
			groupParams = ShapeFactory.INST.createGroup();
			groupParams.addShape(ShapeFactory.INST.createRectangle());
			groupParams.addShape(ShapeFactory.INST.createDot(ShapeFactory.INST.createPoint()));
			groupParams.addShape(ShapeFactory.INST.createGrid(ShapeFactory.INST.createPoint()));
			groupParams.addShape(ShapeFactory.INST.createAxes(ShapeFactory.INST.createPoint()));
			groupParams.addShape(ShapeFactory.INST.createText());
			groupParams.addShape(ShapeFactory.INST.createCircleArc());
			groupParams.addShape(ShapeFactory.INST.createPolyline(Collections.emptyList()));
			groupParams.addShape(ShapeFactory.INST.createBezierCurve(Collections.emptyList()));
			groupParams.addShape(ShapeFactory.INST.createFreeHand(Collections.emptyList()));
			groupParams.addShape(ShapeFactory.INST.createPlot(ShapeFactory.INST.createPoint(), 1, 10, "x", false));
		}
		return groupParams;
	}

	@Override
	public void setActivated(boolean act) {
		if(activated != act) {
			super.setActivated(act);
		}
	}

	@Override
	public void interimFeedback() {
		canvas.setTempView(null);
		canvas.setCursor(Cursor.DEFAULT);
	}

	@Override
	protected void configureBindings() throws IllegalAccessException, InstantiationException {
		addBinding(new Hand.DnD2MoveViewport(this));
		addBinding(new Press2AddShape(this));
		addBinding(new Press2AddText(this));
		addBinding(new Press2InsertPicture(this));
		addBinding(new DnD2AddShape(this));
		addBinding(new MultiClic2AddShape(this));
		addBinding(new Press2InitTextSetter(this));
	}

	/**
	 * @return An instance of a shape configured (thickness, colours, etc.) with the parameters of the pencil.
	 * @since 3.0
	 */
	public IShape createShapeInstance() {
		return setShapeParameters(currentChoice.createShapeInstance());
	}

	/**
	 * Configures the given shape with the parameters (e.g. thickness, colours, etc.) of the pencil.
	 * @param shape The shape to configure.
	 * @return The modified shape given as argument.
	 * @since 3.0
	 */
	IShape setShapeParameters(final IShape shape) {
		shape.copy(getGroupParams());
		shape.setModified(true);
		return shape;
	}


	/** @return The current editing choice. */
	public EditionChoice getCurrentChoice() {
		return currentChoice;
	}

	/**
	 * Sets the current editing choice.
	 * @param choice The new editing choice to set.
	 * @since 3.0
	 */
	public void setCurrentChoice(EditionChoice choice) {
		currentChoice = choice;
	}


	private abstract static class PencilInteractor<I extends JfxInteraction> extends JfXWidgetBinding<AddShape, I, Pencil> {
		PencilInteractor(final I interaction, final Pencil pencil) throws InstantiationException, IllegalAccessException {
			super(pencil, false, AddShape.class, interaction, pencil.canvas);
		}

		ViewShape<?> tmpShape;

		@Override
		public void initAction() {
			final IShape sh = instrument.createShapeInstance();

			tmpShape = null;

			ViewFactory.INSTANCE.createView(sh).ifPresent(v -> {
				tmpShape = v;
				action.setShape(sh);
				action.setDrawing(instrument.canvas.getDrawing());
				instrument.canvas.setTempView(tmpShape);
				Platform.runLater(() -> instrument.canvas.requestFocus());
			});
		}

		void replaceShape(final IShape newSh) {
			if(tmpShape != null) {
				tmpShape.flush();
				tmpShape = null;
			}
			ViewFactory.INSTANCE.createView(newSh).ifPresent(v -> {
				tmpShape = v;
				action.setShape(newSh);
				instrument.canvas.setTempView(tmpShape);
			});
		}
	}


	private static class DnD2AddShape extends PencilInteractor<AbortableDnD> {
		DnD2AddShape(final Pencil pencil) throws InstantiationException, IllegalAccessException {
			super(new AbortableDnD(), pencil);
		}

		@Override
		public void initAction() {
			super.initAction();
			action.getShape().ifPresent(sh -> interaction.getSrcPoint().ifPresent(startPt -> {
				final IPoint pt = instrument.getAdaptedPoint(startPt);

				// For squares and circles, the centre of the shape is the reference point during the creation.
				if(sh instanceof ISquaredShape) {
					final ISquaredShape sq = (ISquaredShape) sh;
					sq.setPosition(pt.getX() - 1d, pt.getY() - 1d);
					sq.setWidth(2d);
				}else if(sh instanceof IFreehand) {
					sh.getPoints().get(0).setPoint(pt.getX(), pt.getY());
				}else {
					sh.translate(pt.getX(), pt.getY());
				}
			}));
		}

		@Override
		public void updateAction() {
			action.getShape().ifPresent(sh -> interaction.getSrcPoint().ifPresent(srcPt -> interaction.getEndPt().ifPresent(finalPt -> {
				// Getting the points depending on the current zoom.
				final IPoint startPt = instrument.getAdaptedPoint(srcPt);
				final IPoint endPt = instrument.getAdaptedPoint(finalPt);

				if(sh instanceof ISquaredShape) {
					updateShapeFromCentre((ISquaredShape) sh, startPt, endPt.getX());
					sh.setModified(true);
					action.getDrawing().ifPresent(drawing -> drawing.setModified(true));
				}else if(sh instanceof IFreehand) {
					final IPoint last = sh.getPtAt(-1);
					if(!MathUtils.INST.equalsDouble(last.getX(), endPt.getX(), 0.0001) &&
						!MathUtils.INST.equalsDouble(last.getY(), endPt.getY(), 0.0001)) {
						replaceShape(ShapeFactory.INST.createFreeHandFrom((IFreehand) sh, endPt));
					}
				}else if(sh instanceof IRectangularShape) {
					updateShapeFromDiag((IRectangularShape) sh, startPt, endPt);
					sh.setModified(true);
					action.getDrawing().ifPresent(drawing -> drawing.setModified(true));
				}
			})));
		}

		/**
		 * @param shape The shape to analyse.
		 * @return The gap that must respect the pencil to not allow shape to disappear when they are too small.
		 * @since 3.0
		 */
		private double getGap(final IShape shape) {
			// These lines are necessary to avoid shape to disappear. It appends when the borders position is INTO.
			// In this case,the minimum radius must be computed using the thickness and the double size.
			if(shape.isBordersMovable() && shape.getBordersPosition() == BorderPos.INTO)
				return shape.getThickness() + (shape.isDbleBorderable() && shape.hasDbleBord() ? shape.getDbleBordSep() : 0d);
			return 1d;
		}

		private void updateShapeFromCentre(final ISquaredShape shape, final IPoint startPt, final double endX) {
			final double sx = startPt.getX();
			final double radius = Math.max(sx < endX ? endX - sx : sx - endX, getGap(shape));
			shape.setPosition(sx - radius, startPt.getY() + radius);
			shape.setWidth(radius * 2d);
		}

		private void updateShapeFromDiag(final IRectangularShape shape, final IPoint startPt, final IPoint endPt) {
			final double gap = getGap(shape);
			double v1 = startPt.getX();
			double v2 = endPt.getX();
			double tlx = Double.NaN;
			double tly = Double.NaN;
			double brx = Double.NaN;
			double bry = Double.NaN;
			boolean ok = true;

			if(Math.abs(v1 - v2) > gap) {
				if(v1 < v2) {
					brx = v2;
					tlx = v1;
				}else {
					brx = v1;
					tlx = v2;
				}
			}else ok = false;

			v1 = startPt.getY();
			v2 = endPt.getY();

			if(Math.abs(v1 - v2) > gap) {
				if(v1 < v2) {
					bry = v2;
					tly = v1;
				}else {
					bry = v1;
					tly = v2;
				}
			}else ok = false;

			if(ok) {
				shape.setPosition(tlx, bry);
				shape.setWidth(brx - tlx);
				shape.setHeight(bry - tly);
			}
		}

		@Override
		public boolean isConditionRespected() {
			final EditionChoice ec = instrument.currentChoice;
			return interaction.getButton().orElse(MouseButton.NONE) == MouseButton.PRIMARY &&
				(ec == EditionChoice.RECT || ec == EditionChoice.ELLIPSE || ec == EditionChoice.SQUARE ||
				ec == EditionChoice.CIRCLE || ec == EditionChoice.RHOMBUS ||
				ec == EditionChoice.TRIANGLE || ec == EditionChoice.CIRCLE_ARC || ec == EditionChoice.FREE_HAND);
		}
	}


	private static class MultiClic2AddShape extends PencilInteractor<MultiClick> {
		MultiClic2AddShape(final Pencil pencil) throws InstantiationException, IllegalAccessException {
			super(new MultiClick(), pencil);
		}

		// To avoid the overlapping with the DnD2AddShape, the starting interaction must be
		// aborted when the condition is not respected, i.e. when the selected shape type is not devoted to the multi-click interaction.
		@Override
		public boolean isInteractionMustBeAborted() {
			return !isConditionRespected();
		}

		@Override
		public void updateAction() {
			action.getShape().ifPresent(sh -> {
				final List<Point3D> pts = interaction.getPoints();
				final IPoint currPoint = instrument.getAdaptedPoint(interaction.getCurrentPosition());

				if(sh.getNbPoints() == pts.size() && !interaction.isLastPointFinalPoint()) {
					final IPoint point = ShapeFactory.INST.createPoint(currPoint.getX(), currPoint.getY());
					if(sh instanceof IPolyline) {
						replaceShape(ShapeFactory.INST.createPolylineFrom((IPolyline) sh, point));
					}else if(sh instanceof IPolygon) {
						replaceShape(ShapeFactory.INST.createPolygonFrom((IPolygon) sh, point));
					}else if(sh instanceof IBezierCurve) {
						replaceShape(ShapeFactory.INST.createBezierCurveFrom((IBezierCurve) sh, point));
					}
				}else {
					((IModifiablePointsShape) sh).setPoint(currPoint.getX(), currPoint.getY(), -1);
				}

				// Curves need to be balanced.
				if(sh instanceof IControlPointShape) {
					((IControlPointShape) sh).balance();
				}

				sh.setModified(true);
				action.getDrawing().ifPresent(dr -> dr.setModified(true));
			});
		}

		@Override
		public void initAction() {
			super.initAction();
			action.getShape().ifPresent(shape -> {
				if(shape instanceof IModifiablePointsShape) {
					final IModifiablePointsShape modShape = (IModifiablePointsShape) shape;
					final IPoint pt = instrument.getAdaptedPoint(interaction.getPoints().get(0));
					modShape.setPoint(pt.getX(), pt.getY(), 0);
					modShape.setPoint(pt.getX() + 1d, pt.getY() + 1d, 1);
				}
			});
		}

		@Override
		public boolean isConditionRespected() {
			return instrument.currentChoice == EditionChoice.POLYGON || instrument.currentChoice == EditionChoice.LINES ||
				instrument.currentChoice == EditionChoice.BEZIER_CURVE || instrument.currentChoice == EditionChoice.BEZIER_CURVE_CLOSED;
		}

		@Override
		public void interactionStarts(final Interaction inter) throws MustAbortStateMachineException {
			super.interactionStarts(inter);
			interaction.setMinPoints(instrument.currentChoice == EditionChoice.POLYGON ? 3 : 2);
		}
	}


	private static class Press2InsertPicture extends JfXWidgetBinding<InsertPicture, Press, Pencil> {
		Press2InsertPicture(final Pencil pencil) throws InstantiationException, IllegalAccessException {
			super(pencil, false, InsertPicture.class, new Press(), pencil.canvas);
		}

		@Override
		public void initAction() {
			interaction.getSrcPoint().ifPresent(srcPt -> {
				action.setDrawing(instrument.canvas.getDrawing());
				action.setShape(ShapeFactory.INST.createPicture(instrument.getAdaptedPoint(srcPt)));
				action.setFileChooser(instrument.getPictureFileChooser());
			});
		}

		@Override
		public boolean isConditionRespected() {
			return instrument.currentChoice == EditionChoice.PICTURE && interaction.getButton().orElse(MouseButton.NONE) == MouseButton.PRIMARY;
		}
	}


	private static class Press2AddShape extends PencilInteractor<Press> {
		Press2AddShape(final Pencil pencil) throws InstantiationException, IllegalAccessException {
			super(new Press(), pencil);
		}

		@Override
		public void initAction() {
			super.initAction();
			action.getShape().ifPresent(sh -> interaction.getSrcPoint().ifPresent(srcPt -> {
				if(sh instanceof IPositionShape) {
					((IPositionShape) sh).setPosition(instrument.getAdaptedPoint(srcPt));
					sh.setModified(true);
				}
			}));
		}

		@Override
		public boolean isConditionRespected() {
			return (instrument.currentChoice == EditionChoice.GRID || instrument.currentChoice == EditionChoice.DOT ||
				instrument.currentChoice == EditionChoice.AXES) && interaction.getButton().orElse(MouseButton.NONE) == MouseButton.PRIMARY;
		}
	}


	 /**
	 * When a user starts to type a text using the text setter and then he clicks somewhere else
	 * in the canvas, the text typed must be added (if possible to the canvas) before starting typing a new text.
	 */
	 private class Press2AddText extends PencilInteractor<Press> {
		 Press2AddText(final Pencil pencil) throws InstantiationException, IllegalAccessException {
			 super(new Press(), pencil);
		 }

		 @Override
		 public void initAction() {
			 action.setDrawing(instrument.canvas.getDrawing());
			 action.setShape(ShapeFactory.INST.createText(ShapeFactory.INST.createPoint(instrument.textSetter.getPosition()),
			 					instrument.textSetter.getTextField().getText()));
		 }

		 // The action is created only when the user uses the text setter and the text field of the text setter is not empty.
		 @Override
		 public boolean isConditionRespected() {
			 return instrument.currentChoice == EditionChoice.TEXT && instrument.textSetter.isActivated() &&
				 !instrument.textSetter.getTextField().getText().isEmpty();
		 }
	 }


	 private static class Press2InitTextSetter extends JfXWidgetBinding<InitTextSetter, Press, Pencil> {
		 Press2InitTextSetter(final Pencil pencil) throws IllegalAccessException, InstantiationException {
			 super(pencil, false, InitTextSetter.class, new Press(), pencil.canvas);
		 }

		 @Override
		 public void initAction() {
			 interaction.getSrcPoint().ifPresent(srcPt -> {
				 action.setText("");
				 action.setTextShape(null);
				 action.setInstrument(instrument.textSetter);
				 action.setTextSetter(instrument.textSetter);
				 action.setPosition(instrument.getAdaptedPoint(srcPt));
			 });
		 }

		 @Override
		 public boolean isConditionRespected() {
			 return (instrument.currentChoice == EditionChoice.TEXT || instrument.currentChoice == EditionChoice.PLOT) &&
				 	interaction.getButton().orElse(MouseButton.NONE) == MouseButton.PRIMARY;
		 }
	 }
}
