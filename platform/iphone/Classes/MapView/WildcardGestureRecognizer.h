//
//  WildcardGestureRecognizer.h
//  Copyright 2010 Floatopian LLC. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "MapViewController.h"

typedef void (^TouchesEventBlock)(NSSet * touches, UIEvent * event);

@interface WildcardGestureRecognizer : UIGestureRecognizer {
	TouchesEventBlock touchesBeganCallback;
	//agregado por mi
	MapViewController *mapController;
	//SEL selector;
}
+ (void) setCallback:(char*) callback;

@property(copy) TouchesEventBlock touchesBeganCallback;
//agregado por mi

@end
