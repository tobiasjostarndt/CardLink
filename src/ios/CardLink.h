#import «Cordova/CDVPlugin.h>

@interface MyCordovaPlugin : CDVPlugin {
｝

// The hooks for our plugin commands
- (void) cardlink: (CDVInvokedUrlCommand *) command;

@end