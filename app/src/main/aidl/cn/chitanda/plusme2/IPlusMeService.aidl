// IPlusMeService.aidl
package cn.chitanda.plusme2;

// Declare any non-default types here with import statements

interface IPlusMeService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void setWelcomePanelSize( in int width, in int height);
    int getWelcomePanelWidth();
    int getWelcomePanelHeight();
}