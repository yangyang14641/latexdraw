package net.sf.latexdraw.instrument.hand;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.sf.latexdraw.instrument.CompositeGUIVoidCommand;
import net.sf.latexdraw.instrument.Hand;
import net.sf.latexdraw.instrument.MetaShapeCustomiser;
import net.sf.latexdraw.instrument.Pencil;
import net.sf.latexdraw.instrument.ShapePropInjector;
import net.sf.latexdraw.instrument.ShapeShadowCustomiser;
import net.sf.latexdraw.instrument.TestShadowStyleGUI;
import net.sf.latexdraw.instrument.TextSetter;
import net.sf.latexdraw.util.Injector;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class TestHandShadowStyle extends TestShadowStyleGUI {
	@Override
	protected Injector createInjector() {
		return new ShapePropInjector() {
			@Override
			protected void configure() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
				super.configure();
				bindToSupplier(Stage.class, () -> stage);
				pencil = mock(Pencil.class);
				bindToInstance(Pencil.class, pencil);
				bindToInstance(TextSetter.class, mock(TextSetter.class));
				bindAsEagerSingleton(Hand.class);
				bindAsEagerSingleton(ShapeShadowCustomiser.class);
				bindToInstance(MetaShapeCustomiser.class, mock(MetaShapeCustomiser.class));
			}
		};
	}

	@Test
	public void testControllerNotActivatedWhenSelectionEmpty() {
		new CompositeGUIVoidCommand(activateHand, updateIns, checkInsDeactivated).execute();
	}

	@Test
	public void testControllerActivatedWhenSelectionGrid() {
		new CompositeGUIVoidCommand(selectionAddRec, activateHand, updateIns).execute();
		assertTrue(ins.isActivated());
		assertTrue(titledPane.isVisible());
	}

	@Test
	public void testControllerDeactivatedWhenSelectionNotGrid() {
		new CompositeGUIVoidCommand(selectionAddGrid, activateHand, updateIns).execute();
		assertFalse(ins.isActivated());
		assertFalse(titledPane.isVisible());
	}

	@Test
	public void testControllerDeactivatedWhenSelectionEmpty() {
		new CompositeGUIVoidCommand(activateHand, updateIns).execute();
		assertFalse(ins.isActivated());
		assertFalse(titledPane.isVisible());
	}

	@Test
	public void testSelectShadowCBHand() {
		new CompositeGUIVoidCommand(activateHand, selectionAddGrid, selectionAddRec, selectionAddRec, updateIns).execute();
		final boolean sel = shadowCB.isSelected();
		checkShadow.execute();
		waitFXEvents.execute();
		assertEquals(shadowCB.isSelected(), drawing.getSelection().getShapeAt(1).orElseThrow().hasShadow());
		assertEquals(shadowCB.isSelected(), drawing.getSelection().getShapeAt(2).orElseThrow().hasShadow());
		assertNotEquals(sel, shadowCB.isSelected());
	}

	@Test
	public void testPickShadowColourHand() {
		new CompositeGUIVoidCommand(activateHand, selectionAddGrid, selectionAddRec, selectionAddRec, checkShadow, updateIns).execute();
		final Color col = shadowColB.getValue();
		pickShadCol.execute();
		waitFXEvents.execute();
		assertEquals(shadowColB.getValue(), drawing.getSelection().getShapeAt(1).orElseThrow().getShadowCol().toJFX());
		assertEquals(shadowColB.getValue(), drawing.getSelection().getShapeAt(2).orElseThrow().getShadowCol().toJFX());
		assertNotEquals(col, shadowColB.getValue());
	}

	@Test
	public void testIncrementShadowSizeHand() {
		doTestSpinner(new CompositeGUIVoidCommand(activateHand, selectionAddGrid, selectionAddRec, selectionAddRec, checkShadow, updateIns), shadowSizeField,
			incrementshadowSizeField, Arrays.asList(
			() ->  drawing.getSelection().getShapeAt(1).orElseThrow().getShadowSize(),
			() ->  drawing.getSelection().getShapeAt(2).orElseThrow().getShadowSize()));
	}

	@Test
	public void testIncrementShadowAngleHand() {
		doTestSpinner(new CompositeGUIVoidCommand(activateHand, selectionAddGrid, selectionAddRec, selectionAddRec, checkShadow, updateIns), shadowAngleField,
			incrementshadowAngleField, Arrays.asList(
			() ->  Math.toDegrees(drawing.getSelection().getShapeAt(1).orElseThrow().getShadowAngle()),
			() ->  Math.toDegrees(drawing.getSelection().getShapeAt(2).orElseThrow().getShadowAngle())));
	}
}
