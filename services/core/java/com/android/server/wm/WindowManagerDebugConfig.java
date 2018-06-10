/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.android.server.wm;

/**
 * Common class for the various debug {@link android.util.Log} output configuration in the window
 * manager package.
 */
public class WindowManagerDebugConfig {
    // All output logs in the window manager package use the {@link #TAG_WM} string for tagging
    // their log output. This makes it easy to identify the origin of the log message when sifting
    // through a large amount of log output from multiple sources. However, it also makes trying
    // to figure-out the origin of a log message while debugging the window manager a little
    // painful. By setting this constant to true, log messages from the window manager package
    // will be tagged with their class names instead fot the generic tag.
    static final boolean TAG_WITH_CLASS_NAME = false;

    // Default log tag for the window manager package.
    static final String TAG_WM = "WindowManager";

    static final boolean DEBUG_RESIZE = true;
    static final boolean DEBUG = true;
    static final boolean DEBUG_ADD_REMOVE = true;
    static final boolean DEBUG_FOCUS = true;
    static final boolean DEBUG_FOCUS_LIGHT = DEBUG_FOCUS || true;
    static final boolean DEBUG_ANIM = true;
    static final boolean DEBUG_KEYGUARD = true;
    static final boolean DEBUG_LAYOUT = true;
    static final boolean DEBUG_LAYERS = true;
    static final boolean DEBUG_INPUT = true;
    static final boolean DEBUG_INPUT_METHOD = true;
    static final boolean DEBUG_VISIBILITY = true;
    static final boolean DEBUG_WINDOW_MOVEMENT = true;
    static final boolean DEBUG_TOKEN_MOVEMENT = true;
    static final boolean DEBUG_ORIENTATION = true;
    static final boolean DEBUG_APP_ORIENTATION = true;
    static final boolean DEBUG_CONFIGURATION = true;
    static final boolean DEBUG_APP_TRANSITIONS = true;
    static final boolean DEBUG_STARTING_WINDOW = true;
    static final boolean DEBUG_WALLPAPER = true;
    static final boolean DEBUG_WALLPAPER_LIGHT = true || DEBUG_WALLPAPER;
    static final boolean DEBUG_DRAG = true;
    static final boolean DEBUG_SCREEN_ON = true;
    static final boolean DEBUG_SCREENSHOT = true;
    static final boolean DEBUG_BOOT = true;
    static final boolean DEBUG_LAYOUT_REPEATS = true;
    static final boolean DEBUG_SURFACE_TRACE = true;
    static final boolean DEBUG_WINDOW_TRACE = true;
    static final boolean DEBUG_TASK_MOVEMENT = true;
    static final boolean DEBUG_TASK_POSITIONING = true;
    static final boolean DEBUG_STACK = true;
    static final boolean DEBUG_DISPLAY = true;
    static final boolean DEBUG_POWER = true;
    static final boolean DEBUG_DIM_LAYER = true;
    static final boolean SHOW_SURFACE_ALLOC = true;
    static final boolean SHOW_TRANSACTIONS = true;
    static final boolean SHOW_VERBOSE_TRANSACTIONS = true && SHOW_TRANSACTIONS;
    static final boolean SHOW_LIGHT_TRANSACTIONS = true || SHOW_TRANSACTIONS;
    static final boolean SHOW_STACK_CRAWLS = true;
    static final boolean DEBUG_WINDOW_CROP = true;

    static final String TAG_KEEP_SCREEN_ON = "DebugKeepScreenOn";
    static final boolean DEBUG_KEEP_SCREEN_ON = false;
}
