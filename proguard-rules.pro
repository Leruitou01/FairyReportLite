# Only rename project code; preserve behavior and reflection entry points.
-dontshrink
-dontoptimize
-keepattributes Exceptions,Signature,InnerClasses,EnclosingMethod,*Annotation*

# The proxy loads this class by the name in bungee.yml.
-keep public class fairylands.fairyreport.Main {
    public <init>();
    public void onEnable();
    public void onDisable();
}

# Bungee discovers event handlers through runtime annotations.
-keepclassmembers class * {
    @net.md_5.bungee.event.EventHandler <methods>;
}

# Preserve overrides and implementations called by external libraries.
-keepclassmembers class * extends net.md_5.bungee.api.plugin.Command {
    public void execute(net.md_5.bungee.api.CommandSender,java.lang.String[]);
}
-keepclassmembers class * implements net.md_5.bungee.api.plugin.TabExecutor {
    public java.lang.Iterable onTabComplete(net.md_5.bungee.api.CommandSender,java.lang.String[]);
}
-keepclassmembers class * implements com.sun.net.httpserver.HttpHandler {
    public void handle(com.sun.net.httpserver.HttpExchange);
}
-keepclassmembers class * implements java.lang.Runnable {
    public void run();
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
