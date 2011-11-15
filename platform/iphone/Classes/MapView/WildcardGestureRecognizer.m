//
//  WildcardGestureRecognizer.m
//  Created by Raymond Daly on 10/31/10.
//  Copyright 2010 Floatopian LLC. All rights reserved.

//

#import "Rhodes.h"
#import "AppManager.h"
#import "common/RhodesApp.h"

#include "logging/RhoLog.h"
#include "ruby/ext/rho/rhoruby.h"


#import "WildcardGestureRecognizer.h"
#import "MapViewController.h"

static NSString* theCallback = nil;

@implementation WildcardGestureRecognizer
@synthesize touchesBeganCallback;

const char* theMapPreload;
-(id) init{
 if (self = [super init])
 {
  self.cancelsTouchesInView = NO;
 }
 return self;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
 if (touchesBeganCallback)
  touchesBeganCallback(touches, event);
	//RAWLOG_ERROR("CAPTURE EVENTO INICIAL!");
	
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
	
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{

	RAWLOG_INFO("Deb0");
	double latitu = (double)[MapViewController centerLatitude2];
	double longitu = (double)[MapViewController centerLongitude2];
	RAWLOG_INFO("Deb1");
	RAWLOG_INFO1("Checkeando callback %s",[theCallback cStringUsingEncoding:NSUTF8StringEncoding] );
	
	 NSString* strBody = @"";
	 NSString* stringLatitude = [NSString stringWithFormat:@"%f", latitu];
	 NSString* stringLongitude = [NSString stringWithFormat:@"%f", longitu];
	 strBody = [strBody stringByAppendingString:@"&latitude="];
	 strBody = [strBody stringByAppendingString:stringLatitude];
	 strBody = [strBody stringByAppendingString:@"&longitude="];
	 strBody = [strBody stringByAppendingString:stringLongitude];
	RAWLOG_INFO("Deb2");
	const char* b = [strBody UTF8String];
	RAWLOG_INFO("Deb2.5");
	 char* norm_url = rho_http_normalizeurl([theCallback cStringUsingEncoding:NSUTF8StringEncoding]);
	//const char *ptr = [theCallback cStringUsingEncoding:NSUTF8StringEncoding];
	
	RAWLOG_INFO1("PROBANDO EL CALLBACK ESTATICO %s",norm_url );
	 rho_net_request_with_data(norm_url, b);
	 rho_http_free(norm_url);
	RAWLOG_INFO("Deb3");

}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
	//RAWLOG_ERROR("CAPTURE EVENTO MOVE!");
}

- (void)reset
{
}

- (void)ignoreTouch:(UITouch *)touch forEvent:(UIEvent *)event
{
}

- (BOOL)canBePreventedByGestureRecognizer:(UIGestureRecognizer *)preventingGestureRecognizer
{
 return NO;
}

- (BOOL)canPreventGestureRecognizer:(UIGestureRecognizer *)preventedGestureRecognizer
{
 return NO;
}
- (void) setMapView:(MapViewController*)theObj {
	mapController = theObj;
}

+ (void) setCallback:(char*) callback {
	RAWLOG_INFO1("PASANDO EL CALLBACK A VARIABLE ESTATICA %s",callback );
	theCallback = [NSString stringWithUTF8String:callback];
    RAWLOG_INFO("Lo setie ");
	

}
/*static void callPreloadCallback(const char* callback, const char* latitudeSpan, int progress) {
    char body[2048];
	
    snprintf(body, sizeof(body), "&rho_callback=1&status=%s&progress=%d", status, progress);
    rho_net_request_with_data(RHODESAPP().canonicalizeRhoUrl(callback).c_str(), body);    
}*/
/*- (void)doSomethingAndNotifyObject:(id)object withSelector:(SEL)selector {
    //do lots of stuff
    [object performSelector:selector withObject:self];
}
*/




@end