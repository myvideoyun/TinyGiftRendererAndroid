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

### 礼物大小对比

Gifts| Components |  vwa Size(KB) | PSNR | Png Size(KB) 
------|-----|---- | ----- | -----
春天的蘑菇 | 蜜蜂 | 28 | 60.73 | 39 
春天的蘑菇 | 蘑菇 | 36 | 48.64 | 1,446 
春天的蘑菇 | 草地 |  142 | 45.151 |986
花草 | top |  66 | 55.818 | 622
花草 | bottom | 121 | 53.93 | 760
花草 | backgroud |  85 | 55.721 | 398
雨伞 | san |226 | 45.683 | 1,144
小红花 | 小红花 | 501 | 45.09 | 2,250
桃树 | 桃树 |  50 | 45.003 | 737 
桃树 | 花瓣 |  39 | 55.141 | 684
仲夏之夜 | 仲夏夜 |  854 | 44.95 | 2,869

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

3. 设置资源路径
```
renderer->setGiftPath(assetPath);
```
Note: 需要在GL线程中设置

4. 渲染
```
renderer->renderGift(inputTextureId, width, height);
```
