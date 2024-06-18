package com.terraformersmc.modmenu.api;


import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

public interface ScreenFactory extends Function<Screen, Screen> {

}
