//
//  RNHeyteaDownloader.m
//  heyteago
//
//  Created by Chris Zhou on 2020/6/15.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import "RNHeyteaDownloader.h"
#import "SSZipArchive.h"
#import "MD5Manager.h"
#import <React/RCTConvert.h>

#define ReloadBundle   @"ReloadBundle"
#define HotUpdatePath  @"HotUpdateBundle"

@implementation RNHeyteaDownloader

+(RNHeyteaDownloader *)instance{  
    static RNHeyteaDownloader * aInstance = nil;
    static dispatch_once_t  once_token;
    dispatch_once(&once_token, ^{
      aInstance = [[RNHeyteaDownloader alloc] init];
    });
    return aInstance;
}


/**
 下载方法
 data {url versionCode md5  forceUpdate}
 */
-(void)downloadWithData:(NSDictionary *)data withResult:(nonnull ResultBlock)block withProgress:(nonnull ProgressBlock)progress{
  // 下载
  self.url = data[@"url"];
  self.versionCode = data[@"versionCode"];
  self.md5 = data[@"md5"];
  self.resBlock = block;
  self.proBlock = progress;
  
  NSURLSession *session = [NSURLSession sessionWithConfiguration:[NSURLSessionConfiguration defaultSessionConfiguration] delegate:self delegateQueue:[NSOperationQueue mainQueue]];
  NSURL *url = [NSURL URLWithString:self.url];
  NSURLRequest *request = [NSURLRequest requestWithURL:url];
  NSURLSessionDownloadTask *task = [session downloadTaskWithRequest:request];
  [task resume];
}



// 下载成功
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask
didFinishDownloadingToURL:(NSURL *)location{
  
  // 保存在 HotUpdateBundle/bundles 下
  // path 和code。 保存到字典 存入数组 写入 bundle.plist
  BOOL isDir = NO;
  NSFileManager *fm = [NSFileManager defaultManager];

  NSString *hotPath = [self getHotUpdatePath];
  NSString *hotBundlesPath = [hotPath stringByAppendingPathComponent:@"bundles"];
  
   if (![fm fileExistsAtPath:hotBundlesPath isDirectory:&isDir]) {
     [fm createDirectoryAtPath:hotBundlesPath withIntermediateDirectories:YES attributes:nil error:nil];
   }
  
  // 解压到bundles 文件夹下
   NSString *curBundlePath = [hotBundlesPath stringByAppendingPathComponent:self.versionCode];
   [SSZipArchive unzipFileAtPath:location.path toDestination:curBundlePath];
  
  NSString *bundlePath = [curBundlePath stringByAppendingPathComponent:@"/bundle-ios/index/main.jsbundle"];
  NSString *contentStr = [NSString stringWithContentsOfFile:bundlePath encoding:NSUTF8StringEncoding error:nil];
  NSString *md5Str = [MD5Manager md5:contentStr];
  
  if(![md5Str isEqualToString:self.md5]) {
    // 更新plist文件
    [self updateVersionPlist];
    self.resBlock(@"success");
  }else{
    // bundle文件不同 热更失败 移除已下载的文件
    [fm removeItemAtPath:curBundlePath error:nil];
    self.resBlock(@"fail");
  }

}

 // 把版本号 和bundle 路径保存到plist文件
-(void)updateVersionPlist{
  BOOL isDir = NO;
  NSString *appVersion = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleShortVersionString"];
  NSString *appBuild = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleVersion"];
  NSString *plistPath = [self getBundlePlistPath];
  NSFileManager *fm = [NSFileManager defaultManager];
  NSMutableArray *plistArr = [NSMutableArray array];
  if ([fm fileExistsAtPath:plistPath isDirectory:&isDir]) {
    plistArr = [NSMutableArray arrayWithContentsOfFile:plistPath];
      NSDictionary *dic = @{@"version":self.versionCode,@"path":self.versionCode,@"status":@"0",@"appVersion":appVersion,@"appBuild":appBuild };
    [plistArr addObject:dic];
    [plistArr writeToFile:plistPath atomically:YES];
  }else{
    NSDictionary *dic = @{@"version":self.versionCode,@"path":self.versionCode,@"status":@"0",@"appVersion":appVersion,@"appBuild":appBuild };
    [plistArr addObject:dic];
    [plistArr writeToFile:plistPath atomically:YES];
  }
}

// 下载失败
- (void)URLSession:(NSURLSession *)session task:(NSURLSessionTask *)task
didCompleteWithError:(nullable NSError *)error{
  if(error != nil){
    self.resBlock(@"fail");
  }
}

// 下载进度
- (void)URLSession:(NSURLSession *)session downloadTask:(NSURLSessionDownloadTask *)downloadTask
             didWriteData:(int64_t)bytesWritten
        totalBytesWritten:(int64_t)totalBytesWritten
totalBytesExpectedToWrite:(int64_t)totalBytesExpectedToWrite{
  float progress = (float) totalBytesWritten / totalBytesExpectedToWrite;
  self.proBlock(progress);

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


@end
