"""一键运行所有内容爬虫
使用：
    python -m crawlers.run_all                  # 运行全部
    python -m crawlers.run_all fish lure        # 只跑指定模块
    python -m crawlers.run_all --no-image       # 跳过图片抓取（更快）
"""
from __future__ import annotations
import sys
import time

from . import fish_crawler, lure_seeder, guide_crawler

PIPELINE = {
    "fish": fish_crawler.run,
    "lure": lambda: lure_seeder.run(skip_image="--no-image" in sys.argv),
    "guide": guide_crawler.run,
}


def main():
    targets = [a for a in sys.argv[1:] if not a.startswith("--")]
    if not targets:
        targets = list(PIPELINE.keys())

    print("=" * 60)
    print(f"LureCalendar 内容爬虫启动 | 任务: {targets}")
    print("=" * 60)

    for name in targets:
        if name not in PIPELINE:
            print(f"未知任务: {name}, 跳过")
            continue
        print(f"\n>>> 开始任务: {name}")
        start = time.time()
        try:
            PIPELINE[name]()
        except Exception as exc:
            print(f"!!! 任务 {name} 异常: {exc}")
        else:
            print(f"<<< 任务 {name} 完成，耗时 {time.time() - start:.1f}s")

    print("\n所有任务结束。")


if __name__ == "__main__":
    main()
