//
//  RCTUdesk.m
//  heyteago
//
//  Created by Chris Zhou on 2020/6/11.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import "RNHeyteaCodePush.h"
#import "RNHeyteaDownloader.h"
#import <React/RCTConvert.h>

#define ReloadBundle   @"ReloadBundle"
#define HotUpdatePath  @"HotUpdateBundle"
#define HotUpdateProgress @"syncProgress"
#define DevBundlePath @"DevRNBundle"

//static NSURL *currentURL = nil;

@interface RNHeyteaCodePush() <RCTBridgeModule>

@end

@implementation RNHeyteaCodePush


RCT_EXPORT_MODULE(ReactNativeHeyteaCodepush)


-(dispatch_queue_t)methodQueue{
  return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents
{
  return @[HotUpdateProgress];
}

-(void)sendNotificationToJsWithProgress:(NSString *)progress{
  [self sendEventWithName:HotUpdateProgress body: progress];
}

RCT_EXPORT_METHOD(syncHot
                  :(BOOL)restartAfterUpdate
                  :(NSString *)md5
                  :(int)versionCode
                  :(NSString *)url
                  :(RCTResponseSenderBlock)callback
                  ){
  
  NSString *versionStr = [NSString stringWithFormat:@"%d",versionCode];
  NSDictionary *data = @{
    @"md5":md5,
    @"url":url,
    @"versionCode":versionStr,
  };
  
  // 下载
  RNHeyteaDownloader *downloader = [RNHeyteaDownloader instance];
  [downloader downloadWithData:data withResult:^(NSString * _Nonnull code) {
    
    if([code isEqualToString:@"fail"]){
      // 下载失败
      callback(@[@(NO),@(YES)]);
    }else{
     // 下载成功
      callback(@[@(YES),@(NO)]);
      [self postReloadNotification];
    }
  } withProgress:^(NSString *progress) {
    // 下载进度
    [self sendNotificationToJsWithProgress:progress];
    
  }];
  
}


// js端加载成功的回调
// 加载成功才保存进plist文件中 
RCT_EXPORT_METHOD(loadSuccess){
  NSString *plistPath = [self getBundlePlistPath];
  NSMutableArray *arr = [NSMutableArray arrayWithContentsOfFile:plistPath];
  NSDictionary *dic = [arr lastObject];
  [arr removeLastObject];
  [dic setValue:@"1" forKey:@"status"];
  [arr addObject:dic];
  [arr writeToFile:plistPath atomically:YES];
}

 

/**
 判断是否需要热更新
 */
RCT_EXPORT_METHOD(checkForHotUpdate:(int)versionCode
                  :(RCTPromiseResolveBlock) resolve
                  :(RCTPromiseRejectBlock)reject){
  
  NSFileManager *fm = [NSFileManager defaultManager];
  NSString *bundlePlistPath = [self getBundlePlistPath];
  
  if ([fm fileExistsAtPath:bundlePlistPath]) {
    // 热更新过 判断当前的热更新版本小于服务器的版本 则执行更新
    NSMutableArray *bundleArr = [NSMutableArray arrayWithContentsOfFile:bundlePlistPath];
    if(bundleArr.count > 0){
      NSString *currentVersion = [bundleArr lastObject][@"version"];
        if ([currentVersion intValue] < versionCode) {
          resolve(@(YES));
        }else{
          resolve(@(NO));
        }
    }else{
      resolve(@(YES));
    }
  }else{
    // 没有热更新过
    resolve(@(YES));
  }
  
}

/**
 App版本更新
 本地build号< 传过来的 app更新
 */
RCT_EXPORT_METHOD(checkForAppUpdate:(int)versionCode
                  :(RCTPromiseResolveBlock)resolve
                  :(RCTPromiseRejectBlock)reject ){

  int buidCode = [[[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"] intValue];
  if (versionCode > buidCode) {
    // app 版本更新
    resolve(@(YES));
  }else {
    // 热更新
    resolve(@(NO));
  }
}

// 跳转appStore
RCT_EXPORT_METHOD(synciOSApp:(NSString *)url){
  NSURL *appUrl = [NSURL URLWithString:url];
  dispatch_async(dispatch_get_main_queue(), ^{
    if ([[UIApplication sharedApplication] canOpenURL:appUrl]) {
       [[UIApplication sharedApplication] openURL:appUrl];
     }
  });
 
}

// 发送刷新jsbundle通知
-(void)postReloadNotification{
  [[NSNotificationCenter defaultCenter]postNotificationName:ReloadBundle object:nil];
}



// 获取存储bundle数组的plist文件路径
-(NSString *)getBundlePlistPath{
  NSString *docPath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
  NSString *bundlePath = [docPath stringByAppendingPathComponent:@"bundle.plist"];
  return bundlePath;
}

// 获取当前加载热更新包的路径
- (NSString *) getHotUpdatePath{
  NSString *docPath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
  NSString *bundlePath = [docPath stringByAppendingPathComponent:HotUpdatePath];
  return bundlePath;
}


+(NSURL *)getLastBundleURL{
    NSURL *tempUrl;
    #ifdef DEBUG
    //bundle文件加载
    NSString *filePath = [NSHomeDirectory() stringByAppendingString:@"/Documents"];
    NSString *devBundleDir = [filePath stringByAppendingPathComponent:DevBundlePath];
    tempUrl = [NSURL URLWithString:[devBundleDir stringByAppendingPathComponent:@"bundles/bundle-ios/main.jsbundle"]];
    #else
    tempUrl = [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
    #endif
    return tempUrl;
}

+(NSString *)getAppVersion{
    return [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleShortVersionString"];
}

+(NSString *)getAppBuild{
    return [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleVersion"];
}

+(NSURL *)bundleURL{
    
    BOOL isDir = NO;
    NSFileManager *fm = [NSFileManager defaultManager];
    
    // 存储热更版本和路径的plist
    NSString *plistPath = [[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0] stringByAppendingPathComponent:@"bundle.plist"];
    
    // 热更新包存储路径
    NSString *hotBundle = [[[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0] stringByAppendingPathComponent:HotUpdatePath] stringByAppendingPathComponent:@"bundles"];
    
    if ([fm fileExistsAtPath:plistPath isDirectory:&isDir]) {
        NSMutableArray *arr = [NSMutableArray arrayWithContentsOfFile:plistPath];
        // 过滤掉已加载且失败的包 不过滤未加载的
        NSMutableArray *tempArr = [NSMutableArray array];
        if (arr.count > 0) {
            for (NSDictionary *dic in arr) {
                if ([dic[@"status"] isEqualToString:@"0"] && [dic[@"isLoad"] isEqualToString:@"1"]) {
                    // 已加载但未成功 过滤掉
                }else{
                    [tempArr addObject:dic];
                }
            }
            [tempArr writeToFile:plistPath atomically:YES];
        }
        
       
        
        if (tempArr.count > 0) {
            NSDictionary *currentDic = [tempArr lastObject];
            NSString *path = currentDic[@"path"];
            NSString *appVersion = currentDic[@"appVersion"];
            NSString *appBuild = currentDic[@"appBuild"];
            NSString *finalStr = [hotBundle stringByAppendingFormat:@"/%@/bundle-ios/main.jsbundle",path];
            // 存在bundle包 且该bundle包的version build与app当前一致 才去加载热更包
            if ([fm fileExistsAtPath:finalStr] && [appVersion isEqualToString:[self getAppVersion]] && [appBuild isEqualToString:[self getAppBuild]]) {
                
                if([currentDic[@"isLoad"] isEqualToString:@"0"]){
                    //首次加载 设置为已加载
                    [self configIsLoadWithVersion:path];
                }
                return [NSURL URLWithString:finalStr];
            }
        }
    }
    
    return [self getLastBundleURL];
}

// 把第一次加载的包置为已加载
+(void)configIsLoadWithVersion:(NSString *)version{
    // 存储热更版本和路径的plist
    NSString *plistPath = [[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0] stringByAppendingPathComponent:@"bundle.plist"];
    NSMutableArray *arr = [NSMutableArray arrayWithContentsOfFile:plistPath];
    for (NSDictionary *dic in arr) {
        if ([dic[@"path"] isEqualToString:version] && [dic[@"isLoad"] isEqualToString: @"0"]) {
            [dic setValue:@"1" forKey:@"isLoad"];
        }
    }
    // 修改完 isLoad 写入
    [arr writeToFile:plistPath atomically:YES];
}


// 获取当前的viewcontroller
-(UIViewController *) getCurrentViewController{
  UIViewController *currentViewController = [[[[UIApplication sharedApplication] delegate] window] rootViewController];
  BOOL runLoopFind = YES;
     while (runLoopFind) {
         if (currentViewController.presentedViewController) {
             currentViewController = currentViewController.presentedViewController;
         } else {
             if ([currentViewController isKindOfClass:[UINavigationController class]]) {
                 currentViewController = ((UINavigationController *)currentViewController).visibleViewController;
             } else if ([currentViewController isKindOfClass:[UITabBarController class]]) {
                 currentViewController = ((UITabBarController* )currentViewController).selectedViewController;
             } else {
                 break;
             }
         }
     }
     return currentViewController;
}

@end
