import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";

export default async function NotFound() {
  const t = await getTranslations("NotFound");

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 px-4 text-center">
      <h1 className="text-2xl font-bold tracking-tight">{t("Title")}</h1>
      <p className="text-sm text-zinc-500">{t("Description")}</p>
      <Link
        href="/"
        className="rounded-md border border-zinc-300 bg-white px-4 py-2 text-sm font-medium text-zinc-900 hover:bg-zinc-50"
      >
        {t("BackHome")}
      </Link>
    </div>
  );
}
