# TinyGiftRenderer

  迷你礼物渲染库功能强大，全面兼容安卓IOS，使用简单；

## 功能

1. 渲染贴纸挂件，如猫耳朵
2. 渲染大型礼物素材

## 特点

1. 素材体积小
2. 渲染速度快
3. 扩展灵活，支持新的挂件特效
4. 使用简单
5. 兼容性好
6. 授权方式灵活

## 商务合作
联系人：范经理  
邮箱：marketing@myvideoyun.com  
电话：+8613818445512

## 使用方式
### Android Java
1. 认证鉴权
```
TinyGiftRenderer.auth(getApplicationContext(), your-auth-key-string, your-auth-key-length)
```

2. 创建TinyGiftRenderer
```
int vertFlip = 1;
TinyGiftRenderer renderer = new TinyGiftRenderer(context.getApplicationContext(), vertFlip);
```
Note: 这个需要在GL线程中创建？

3. 设置资源路径
```
renderer->setGiftPath(assetPath);
```
Note: 需要在GL线程中设置

4. 渲染
```
renderer->renderGift(inputTextureId, width, height);
```
