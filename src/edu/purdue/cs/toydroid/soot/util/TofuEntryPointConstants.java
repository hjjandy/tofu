package edu.purdue.cs.toydroid.soot.util;

/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p>
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/

import java.util.Arrays;
import java.util.List;

/**
 * Class containing constants for the well-known Android lifecycle methods
 */
public class TofuEntryPointConstants {
	
	/*========================================================================*/

	public static final String ACTIVITYCLASS = "android.app.Activity";
	public static final String SERVICECLASS = "android.app.Service";
	public static final String GCMBASEINTENTSERVICECLASS = "com.google.android.gcm.GCMBaseIntentService";
	public static final String GCMLISTENERSERVICECLASS = "com.google.android.gms.gcm.GcmListenerService";
	public static final String BROADCASTRECEIVERCLASS = "android.content.BroadcastReceiver";
	public static final String CONTENTPROVIDERCLASS = "android.content.ContentProvider";
	public static final String APPLICATIONCLASS = "android.app.Application";
	public static final String V4FRAGMENTCLASS = "android.support.v4.app.Fragment";
	public static final String FRAGMENTCLASS = "android.app.Fragment";
	public static final String V4PagerAdapter = "android.support.v4.view.PagerAdapter";
	public static final String V4ViewPagerOnPageChangeListener = "android.support.v4.view.ViewPager$OnPageChangeListener";

	public static final String ACTIVITY_ONCREATE = "void onCreate(android.os.Bundle)";
	public static final String ACTIVITY_ONSTART = "void onStart()";
	public static final String ACTIVITY_ONRESTOREINSTANCESTATE = "void onRestoreInstanceState(android.os.Bundle)";
	public static final String ACTIVITY_ONPOSTCREATE = "void onPostCreate(android.os.Bundle)";
	public static final String ACTIVITY_ONRESUME = "void onResume()";
	public static final String ACTIVITY_ONPOSTRESUME = "void onPostResume()";
	public static final String ACTIVITY_ONCREATEDESCRIPTION = "java.lang.CharSequence onCreateDescription()";
	public static final String ACTIVITY_ONSAVEINSTANCESTATE = "void onSaveInstanceState(android.os.Bundle)";
	public static final String ACTIVITY_ONPAUSE = "void onPause()";
	public static final String ACTIVITY_ONSTOP = "void onStop()";
	public static final String ACTIVITY_ONRESTART = "void onRestart()";
	public static final String ACTIVITY_ONDESTROY = "void onDestroy()";

	public static final String SERVICE_ONCREATE = "void onCreate()";
	public static final String SERVICE_ONSTART1 = "void onStart(android.content.Intent,int)";
	public static final String SERVICE_ONSTART2 = "int onStartCommand(android.content.Intent,int,int)";
	public static final String SERVICE_ONBIND = "android.os.IBinder onBind(android.content.Intent)";
	public static final String SERVICE_ONREBIND = "void onRebind(android.content.Intent)";
	public static final String SERVICE_ONUNBIND = "boolean onUnbind(android.content.Intent)";
	public static final String SERVICE_ONDESTROY = "void onDestroy()";

	public static final String GCMINTENTSERVICE_ONDELETEDMESSAGES = "void onDeletedMessages(android.content.Context,int)";
	public static final String GCMINTENTSERVICE_ONERROR = "void onError(android.content.Context,java.lang.String)";
	public static final String GCMINTENTSERVICE_ONMESSAGE = "void onMessage(android.content.Context,android.content.Intent)";
	public static final String GCMINTENTSERVICE_ONRECOVERABLEERROR = "void onRecoverableError(android.content.Context,java.lang.String)";
	public static final String GCMINTENTSERVICE_ONREGISTERED = "void onRegistered(android.content.Context,java.lang.String)";
	public static final String GCMINTENTSERVICE_ONUNREGISTERED = "void onUnregistered(android.content.Context,java.lang.String)";

	public static final String GCMLISTENERSERVICE_ONDELETEDMESSAGES = "void onDeletedMessages()";
	public static final String GCMLISTENERSERVICE_ONMESSAGERECEIVED = "void onMessageReceived(java.lang.String,android.os.Bundle)";
	public static final String GCMLISTENERSERVICE_ONMESSAGESENT = "void onMessageSent(java.lang.String)";
	public static final String GCMLISTENERSERVICE_ONSENDERROR = "void onSendError(java.lang.String,java.lang.String)";

	public static final String BROADCAST_ONRECEIVE = "void onReceive(android.content.Context,android.content.Intent)";

	public static final String CONTENTPROVIDER_ONCREATE = "boolean onCreate()";

	public static final String APPLICATION_ONCREATE = "void onCreate()";
	public static final String APPLICATION_ONTERMINATE = "void onTerminate()";

	public static final String APPLIFECYCLECALLBACK_ONACTIVITYSTARTED = "void onActivityStarted(android.app.Activity)";
	public static final String APPLIFECYCLECALLBACK_ONACTIVITYSTOPPED = "void onActivityStopped(android.app.Activity)";
	public static final String APPLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE = "void onActivitySaveInstanceState(android.app.Activity,android.os.Bundle)";
	public static final String APPLIFECYCLECALLBACK_ONACTIVITYRESUMED = "void onActivityResumed(android.app.Activity)";
	public static final String APPLIFECYCLECALLBACK_ONACTIVITYPAUSED = "void onActivityPaused(android.app.Activity)";
	public static final String APPLIFECYCLECALLBACK_ONACTIVITYDESTROYED = "void onActivityDestroyed(android.app.Activity)";
	public static final String APPLIFECYCLECALLBACK_ONACTIVITYCREATED = "void onActivityCreated(android.app.Activity,android.os.Bundle)";

	public static final String V4FRAGMENT_ONATTACH_Activity = "void onAttach(android.app.Activity)";
	public static final String V4FRAGMENT_ONATTACH_Context = "void onAttach(android.content.Context)";
	public static final String V4FRAGMENT_ONCREATE = "void onCreate(android.os.Bundle)";
	public static final String V4FRAGMENT_ONCREATEVIEW = "android.view.View onCreateView(android.view.LayoutInflater,android.view.ViewGroup,android.os.Bundle)";
	public static final String V4FRAGMENT_ONVIEWCREATED = "void onViewCreated(android.view.View,android.os.Bundle)";
	public static final String V4FRAGMENT_ONACTIVITYCREATED = "void onActivityCreated(android.os.Bundle)";
	public static final String V4FRAGMENT_ONSTART = "void onStart()";
	public static final String V4FRAGMENT_ONRESUME = "void onResume()";
	public static final String V4FRAGMENT_ONPAUSE = "void onPause()";
	public static final String V4FRAGMENT_ONSTOP = "void onStop()";
	public static final String V4FRAGMENT_ONDESTROYVIEW = "void onDestroyView()";
	public static final String V4FRAGMENT_ONDESTROY = "void onDestroy()";
	public static final String V4FRAGMENT_ONDETACH = "void onDetach()";

	public static final String V4PagerAdapter_INSTANTIATEITEM_ViewGroup = "java.lang.Object instantiateItem(android.view.ViewGroup,int)";
	//	public static final String V4PagerAdapter_INSTANTIATEITEM_View = "java.lang.Object instantiateItem(android.view.View,int)";
	public static final String V4PagerAdapter_DESTROYITEM_ViewGroup = "void destroyItem(android.view.ViewGroup,int,java.lang.Object)";
//	public static final String V4PagerAdapter_DESTROYITEM_View = "void destroyItem(android.view.View,int,java.lang.Object)";

	public static final String V4ViewPagerOnPageChangeListener_onPageScrollStateChanged = "void onPageScrollStateChanged(int)";
	public static final String V4ViewPagerOnPageChangeListener_onPageScrolled = "void onPageScrolled(int,float,int)";
	public static final String V4ViewPagerOnPageChangeListener_onPageSelected = "void onPageSelected(int)";

	public static final String CustomView_onMeasure = "void onMeasure(int,int)";
	public static final String CustomView_onLayout = "void onLayout(boolean,int,int,int,int)";
	public static final String CustomView_onDraw = "void onDraw(android.graphics.Canvas)";
	
	/*========================================================================*/

	private static final String[] activityMethods = {ACTIVITY_ONCREATE, ACTIVITY_ONDESTROY, ACTIVITY_ONPAUSE, ACTIVITY_ONRESTART, ACTIVITY_ONRESUME, ACTIVITY_ONSTART, ACTIVITY_ONSTOP, ACTIVITY_ONSAVEINSTANCESTATE, ACTIVITY_ONRESTOREINSTANCESTATE, ACTIVITY_ONCREATEDESCRIPTION, ACTIVITY_ONPOSTCREATE, ACTIVITY_ONPOSTRESUME};

	private static final String[] serviceMethods = {SERVICE_ONCREATE, SERVICE_ONDESTROY, SERVICE_ONSTART1, SERVICE_ONSTART2, SERVICE_ONBIND, SERVICE_ONREBIND, SERVICE_ONUNBIND};

	private static final String[] gcmIntentServiceMethods = {GCMINTENTSERVICE_ONDELETEDMESSAGES, GCMINTENTSERVICE_ONERROR, GCMINTENTSERVICE_ONMESSAGE, GCMINTENTSERVICE_ONRECOVERABLEERROR, GCMINTENTSERVICE_ONREGISTERED, GCMINTENTSERVICE_ONUNREGISTERED};

	private static final String[] gcmListenerServiceMethods = {GCMLISTENERSERVICE_ONDELETEDMESSAGES, GCMLISTENERSERVICE_ONMESSAGERECEIVED, GCMLISTENERSERVICE_ONMESSAGESENT, GCMLISTENERSERVICE_ONSENDERROR};

	private static final String[] broadcastMethods = {BROADCAST_ONRECEIVE};

	private static final String[] contentproviderMethods = {CONTENTPROVIDER_ONCREATE};

	private static final String[] applicationMethods = {APPLICATION_ONCREATE, APPLICATION_ONTERMINATE, APPLIFECYCLECALLBACK_ONACTIVITYSTARTED, APPLIFECYCLECALLBACK_ONACTIVITYSTOPPED, APPLIFECYCLECALLBACK_ONACTIVITYSAVEINSTANCESTATE, APPLIFECYCLECALLBACK_ONACTIVITYRESUMED, APPLIFECYCLECALLBACK_ONACTIVITYPAUSED, APPLIFECYCLECALLBACK_ONACTIVITYDESTROYED, APPLIFECYCLECALLBACK_ONACTIVITYCREATED};

	private static final String[] fragmentMethods = {V4FRAGMENT_ONATTACH_Activity, V4FRAGMENT_ONATTACH_Context, V4FRAGMENT_ONCREATE, V4FRAGMENT_ONCREATEVIEW, V4FRAGMENT_ONVIEWCREATED, V4FRAGMENT_ONACTIVITYCREATED, V4FRAGMENT_ONSTART, V4FRAGMENT_ONRESUME, V4FRAGMENT_ONPAUSE, V4FRAGMENT_ONSTOP, V4FRAGMENT_ONDESTROYVIEW, V4FRAGMENT_ONDESTROY, V4FRAGMENT_ONDETACH};

	private static final String[] pagerAdapterMethods = {V4PagerAdapter_INSTANTIATEITEM_ViewGroup, V4PagerAdapter_DESTROYITEM_ViewGroup};

	private static final String[] viewPagerMethods = {V4ViewPagerOnPageChangeListener_onPageScrollStateChanged, V4ViewPagerOnPageChangeListener_onPageScrolled, V4ViewPagerOnPageChangeListener_onPageSelected};

	private static final String[] customeViewMethods = {CustomView_onMeasure, CustomView_onLayout, CustomView_onDraw};
	/*========================================================================*/

	public static List<String> getActivityLifecycleMethods() {
		return Arrays.asList(activityMethods);
	}

	public static List<String> getServiceLifecycleMethods() {
		return Arrays.asList(serviceMethods);
	}

	public static List<String> getGCMIntentServiceMethods() {
		return Arrays.asList(gcmIntentServiceMethods);
	}

	public static List<String> getGCMListenerServiceMethods() {
		return Arrays.asList(gcmListenerServiceMethods);
	}

	public static List<String> getBroadcastLifecycleMethods() {
		return Arrays.asList(broadcastMethods);
	}

	public static List<String> getContentproviderLifecycleMethods() {
		return Arrays.asList(contentproviderMethods);
	}

	public static List<String> getApplicationLifecycleMethods() {
		return Arrays.asList(applicationMethods);
	}

	public static List<String> getFragmentLifecycleMethods() {
		return Arrays.asList(fragmentMethods);
	}

	public static List<String> getPagerAdapterLifecycleMethods() {
		return Arrays.asList(pagerAdapterMethods);
	}

	public static List<String> getViewPagerOnPageChangeListenerLifecycleMethods() {
		return Arrays.asList(viewPagerMethods);
	}

	public static List<String> getCustomViewLifecycleMethods() {
		return Arrays.asList(customeViewMethods);
	}
	/*========================================================================*/

	/**
	 * Gets whether the given class if one of Android's default lifecycle
	 * classes (android.app.Activity etc.)
	 *
	 * @param className The name of the class to check
	 * @return True if the given class is one of Android's default lifecycle
	 * classes, otherwise false
	 */
	public static boolean isLifecycleClass(String className) {
		return className.equals(ACTIVITYCLASS) || className.equals(SERVICECLASS) ||
			   className.equals(BROADCASTRECEIVERCLASS) || className.equals(CONTENTPROVIDERCLASS) ||
			   className.equals(APPLICATIONCLASS) || className.equals(V4FRAGMENTCLASS) ||
			   className.equals(FRAGMENTCLASS) || className.equals(V4PagerAdapter) ||
			   className.equals(V4ViewPagerOnPageChangeListener);
	}

}

