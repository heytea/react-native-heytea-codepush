//
//  RNHeyteaDownloader.h
//  heyteago
//
//  Created by Chris Zhou on 2020/6/15.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef void(^ResultBlock)(NSString * code);
typedef void(^ProgressBlock)(int progress);

@interface RNHeyteaDownloader : NSObject <NSURLSessionDownloadDelegate>

@property(nonatomic,copy) ProgressBlock proBlock;
@property(nonatomic,copy) ResultBlock resBlock;
@property(nonatomic,copy) NSString *url;
@property(nonatomic,copy) NSString *versionCode;
@property(nonatomic,copy)NSString *md5;


+(instancetype)instance;

-(void)downloadWithData:(NSDictionary *)data withResult:(ResultBlock)block withProgress:(ProgressBlock)progress;

@end

NS_ASSUME_NONNULL_END
