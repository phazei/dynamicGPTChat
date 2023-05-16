package com.phazei.dynamicgptchat.swipereveal

/**
The MIT License (MIT)

Copyright (c) 2016 Chau Thai

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

import android.os.Bundle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * [ViewBinderHelper] provides a quick and easy solution to restore the open/close state
 * of the items in RecyclerView, ListView, GridView or any view that requires its child view
 * to bind the view to a data object.
 *
 * Call [bind] when binding your data object to a view, to
 * save and restore the open/close state of the view.
 *
 * Optionally, if you also want to save and restore the open/close state when the device's
 * orientation is changed, call [saveStates] in [android.app.Activity.onSaveInstanceState]
 * and [restoreStates] in [android.app.Activity.onRestoreInstanceState]
 */
class ViewBinderHelper {
    companion object {
        private const val BUNDLE_MAP_KEY = "ViewBinderHelper_Bundle_Map_Key"
    }

    private var mapStates = ConcurrentHashMap<String, Int>()
    private var mapLayouts = ConcurrentHashMap<String, SwipeRevealLayout>()
    private var lockedSwipeSet = CopyOnWriteArraySet<String>()

    private var openOnlyOne = false
    private val stateChangeLock = Any()

    /**
     * Helps to save and restore open/close state of the swipeLayout. Call this method
     * when you bind your view holder with the data object.
     *
     * @param swipeLayout swipeLayout of the current view.
     * @param id a string that uniquely defines the data object of the current view.
     */
    fun bind(swipeLayout: SwipeRevealLayout, id: String) {
        if (swipeLayout.shouldRequestLayout()) {
            swipeLayout.requestLayout()
        }

        mapLayouts.values.remove(swipeLayout)
        mapLayouts[id] = swipeLayout

        swipeLayout.abort()
        swipeLayout.setDragStateChangeListener(object : SwipeRevealLayout.DragStateChangeListener {
            override fun onDragStateChanged(state: Int) {
                mapStates[id] = state

                if (openOnlyOne) {
                    closeOthers(id, swipeLayout)
                }
            }
        })

        // first time binding.
        if (!mapStates.containsKey(id)) {
            mapStates[id] = SwipeRevealLayout.STATE_CLOSE
            swipeLayout.close(false)
        }

        // not the first time, then close or open depends on the current state.
        else {
            val state = mapStates[id]

            if (state == SwipeRevealLayout.STATE_CLOSE || state == SwipeRevealLayout.STATE_CLOSING || state == SwipeRevealLayout.STATE_DRAGGING) {
                swipeLayout.close(false)
            } else {
                swipeLayout.open(false)
            }
        }

        // set lock swipe
        swipeLayout.setLockDrag(lockedSwipeSet.contains(id))
    }

    /**
     * Only if you need to restore open/close state when the orientation is changed.
     * Call this method in [android.app.Activity.onSaveInstanceState]
     */
    fun saveStates(outState: Bundle?) {
        outState?.let {
            val statesBundle = Bundle()
            for ((key, value) in mapStates) {
                statesBundle.putInt(key, value)
            }
            it.putBundle(BUNDLE_MAP_KEY, statesBundle)
        }
    }

    /**
     * Only if you need to restore open/close state when the orientation is changed.
     * Call this method in [android.app.Activity.onRestoreInstanceState]
     */
    fun restoreStates(inState: Bundle?) {
        inState?.let {
            if (it.containsKey(BUNDLE_MAP_KEY)) {
                val statesBundle = it.getBundle(BUNDLE_MAP_KEY)
                val restoredMap = HashMap<String, Int>()
                statesBundle?.keySet()?.forEach { key ->
                    restoredMap[key] = statesBundle.getInt(key)
                }
                mapStates = ConcurrentHashMap(restoredMap)
            }
        }
    }

    /**
     * Lock swipe for some layouts.
     * @param id a string that uniquely defines the data object.
     */
    fun lockSwipe(vararg id: String) {
        setLockSwipe(true, *id)
    }

    /**
     * Unlock swipe for some layouts.
     * @param id a string that uniquely defines the data object.
     */
    fun unlockSwipe(vararg id: String) {
        setLockSwipe(false, *id)
    }

    /**
     * If set to true, then only one row can be opened at a time.
     */
    fun setOpenOnlyOne(openOnlyOne: Boolean) {
        this.openOnlyOne = openOnlyOne
    }

    /**
     * Open a specific layout.
     * @param id unique id which identifies the data object which is bind to the layout.
     */
    fun openLayout(id: String) {
        synchronized(stateChangeLock) {
            mapStates[id] = SwipeRevealLayout.STATE_OPEN

            mapLayouts[id]?.let {
                it.open(true)
                if (openOnlyOne) {
                    closeOthers(id, it)
                }
            }
        }
    }

    /**
     * Close a specific layout.
     * @param id unique id which identifies the data object which is bind to the layout.
     */
    fun closeLayout(id: String) {
        synchronized(stateChangeLock) {
            mapStates[id] = SwipeRevealLayout.STATE_CLOSE
            mapLayouts[id]?.close(true)
        }
    }

    /**
     * Close others swipe layout.
     * @param id layout which bind with this data object id will be excluded.
     * @param swipeLayout will be excluded.
     */
    private fun closeOthers(id: String, swipeLayout: SwipeRevealLayout) {
        synchronized(stateChangeLock) {
            // close other rows if openOnlyOne is true.
            if (getOpenCount() > 1) {
                for ((key) in mapStates) {
                    if (key != id) {
                        mapStates[key] = SwipeRevealLayout.STATE_CLOSE
                    }
                }

                for ((_, layout) in mapLayouts) {
                    if (layout != swipeLayout) {
                        layout.close(true)
                    }
                }
            }
        }
    }

    private fun setLockSwipe(lock: Boolean, vararg id: String)
    {
        id.forEach {
            if (lock)
                lockedSwipeSet.add(it)
            else
                lockedSwipeSet.remove(it)

            mapLayouts[it]?.setLockDrag(lock)
        }
    }

    private fun getOpenCount(): Int {
        var total = 0
        for (state in mapStates.values) {
            if (state == SwipeRevealLayout.STATE_OPEN || state == SwipeRevealLayout.STATE_OPENING) {
                total++
            }
        }
        return total
    }
}
